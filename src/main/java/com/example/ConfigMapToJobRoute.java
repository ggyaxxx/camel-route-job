package com.example;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.utils.Serialization;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ConfigMapToJobRoute extends RouteBuilder {

    @Autowired
    private final KubernetesClient kubernetesClient;

    public ConfigMapToJobRoute(KubernetesClient kubernetesClient) {
        this.kubernetesClient = kubernetesClient;
    }

    @Value("${myapp.camel.file.input.directory:input}")
    private String inputDirectory;

    @Value("${myapp.camel.file.processing.directory:processing}")
    private String processingDirectory;

    @Override
    public void configure() throws Exception {
        from("file:" + inputDirectory +
                "?excludeExt=sh,sql,ctl,split" +
                "&exclude=.*UCEWL.*|ID_.*" +
                "&keepLastModified=true" +
                "&readLock=none" +
                "&noop=true" +
                "&idempotentKey=${file:name}-${file:modified}" +
                "&idempotentRepository=#myFileRepository" )

                .log("File rilevato: ${header.CamelFileNameOriginal}")
//                .log("Pre-mosso in: " + processingDirectory)
                .process(exchange -> {
                    ConfigMap configMap = kubernetesClient.configMaps()
                            .inNamespace("camel-rotta")
                            .withName("job-config")
                            .get();

                    if (configMap != null && configMap.getData() != null) {
                        String jobYaml = configMap.getData().get("job-definition");
                        if (jobYaml != null) {
                            Job job = Serialization.unmarshal(jobYaml, Job.class);
                            String fileName = exchange.getIn().getHeader("CamelFileName", String.class);
                            String jobName = "camel-job-" + fileName.replaceAll("\\W+", "-").toLowerCase() + "-" + (int) (Math.random() * 9000 + 1000);
                            job.getMetadata().setName(jobName);
                            job.getMetadata().setNamespace("camel-rotta");

                            kubernetesClient.batch().v1().jobs()
                                    .inNamespace("camel-rotta")
                                    .create(job);

                            exchange.getMessage().setBody("Job creato con successo: " + job.getMetadata().getName());
                        } else {
                            throw new RuntimeException("La chiave 'job-definition' non è presente nella ConfigMap");
                        }
                    } else {
                        throw new RuntimeException("ConfigMap non trovata o dati mancanti");
                    }
                })
                .log("${body}");

    }
}

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
        from("timer:configMapTimer?period=60000")
                .log("Fetching ConfigMap from Kubernetes...")
                .process(exchange -> {
                    // Recupera la ConfigMap
                    ConfigMap configMap = kubernetesClient.configMaps()
                            .inNamespace("camel-rotta")
                            .withName("job-config")
                            .get();

                    if (configMap != null && configMap.getData() != null) {
                        String jobYaml = configMap.getData().get("job-definition");
                        if (jobYaml != null) {
                            Job job = Serialization.unmarshal(jobYaml, Job.class);
                            job.getMetadata().setName("camel-job-" + (int) (Math.random() * 9000 + 1000));
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

        from("file:" + inputDirectory +
                "?preMove=" + processingDirectory + "/${file:name}" +
                "&readLock=changed" +
                "&readLockCheckInterval=1000" +
                "&readLockMinLength=1" +
                "&readLockMinAge=2000" +
                "&delete=false" +
                "&noop=true" +
                "&initialDelay=1000&delay=5000")

                .log("File originale ${header.CamelFileNameOriginal} pre-mosso in: " + processingDirectory)
                .log("Inizio processamento per il file: ${header.CamelFilePath}") // CamelFilePath punta al file in processingDirectory


                .log("Avvio del job Kubernetes per il file ${header.CamelFileNameOriginal}");

    }
}

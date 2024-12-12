package com.example;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.utils.Serialization;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

@Component
public class ConfigMapToJobRoute extends RouteBuilder {

    private final KubernetesClient kubernetesClient;

    public ConfigMapToJobRoute(KubernetesClient kubernetesClient) {
        this.kubernetesClient = kubernetesClient;
    }

    @Override
    public void configure() throws Exception {
        from("timer:configMapTimer?period=10000")
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
    }
}

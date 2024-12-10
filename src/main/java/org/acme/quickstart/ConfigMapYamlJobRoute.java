package org.acme.quickstart;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.kubernetes.client.utils.Serialization;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.kubernetes.KubernetesConstants;
import org.apache.camel.component.kubernetes.KubernetesOperations;

import java.nio.charset.StandardCharsets;

public class ConfigMapYamlJobRoute extends RouteBuilder {

    @Override
    public void configure() throws Exception {

        from("timer:yamlJobTimer?period=10000") // Trigger ogni 10 secondi
                .routeId("kubernetes-yaml-job")
                .process(exchange -> {
                    try (KubernetesClient client = new KubernetesClientBuilder().build()) {
                        // Recupera la ConfigMap dal cluster Kubernetes
                        ConfigMap configMap = client.configMaps()
                                .inNamespace("camel-rotta")
                                .withName("job-config")
                                .get();

                        if (configMap == null || !configMap.getData().containsKey("job-definition")) {
                            throw new RuntimeException("ConfigMap o chiave 'job-definition' non trovata!");
                        }

                        // Recupera il contenuto YAML dalla chiave 'job-definition'
                        String jobYamlString = configMap.getData().get("job-definition");

                        if (jobYamlString == null || jobYamlString.trim().isEmpty()) {
                            throw new RuntimeException("Il contenuto di 'job-definition' è vuoto!");
                        }

                        // Utilizza Fabric8 Serialization per trasformare lo YAML in un oggetto Job
                        Job job = Serialization.unmarshal(jobYamlString, Job.class);

                        if (job == null) {
                            throw new RuntimeException("Il Job YAML non è valido o non è stato parsato correttamente!");
                        }

                        // Genera un nome unico per il Job
                        String jobName = "manual-trigger-" + System.currentTimeMillis();
                        if (job.getMetadata() == null) {
                            job.setMetadata(new io.fabric8.kubernetes.api.model.ObjectMeta());
                        }
                        job.getMetadata().setName(jobName);

                        // Passa il Job nel corpo dell'exchange
                        exchange.getMessage().setBody(job);
                        exchange.getMessage().setHeader(KubernetesConstants.KUBERNETES_NAMESPACE_NAME, "camel-rotta");
                    }
                })
                .to("kubernetes-job://kubernetes.default.svc?operation=" + KubernetesOperations.CREATE_JOB_OPERATION) // Crea il Job
                .log("Job creato con successo: ${header." + KubernetesConstants.KUBERNETES_JOB_NAME + "}");
    }
}

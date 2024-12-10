package org.acme.quickstart;

import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.kubernetes.KubernetesConstants;

import java.io.InputStream;

public class LoadYamlJobRoute extends RouteBuilder {

    @Override
    public void configure() throws Exception {

        from("timer:yamlJobTimer?period=10000") // Trigger ogni 10 secondi
                .routeId("kubernetes-yaml-job")
                .process(exchange -> {
                    try (KubernetesClient client = new KubernetesClientBuilder().build()) {
                        // Carica il file YAML dal classpath
                        InputStream jobYaml = LoadYamlJobRoute.class.getResourceAsStream("/job.yaml");

                        if (jobYaml == null) {
                            throw new RuntimeException("File YAML non trovato!");
                        }

                        // Carica il Job dallo YAML
                        Job job = client.batch().v1().jobs().load(jobYaml).get();

                        // Genera un nome unico per il Job
                        String jobName = "manual-trigger-" + System.currentTimeMillis();
                        job.getMetadata().setName(jobName);

                        // Passa il Job al corpo dell'exchange
                        exchange.getMessage().setBody(job);

                        // Imposta il namespace
                        exchange.getMessage().setHeader(KubernetesConstants.KUBERNETES_NAMESPACE_NAME, job.getMetadata().getNamespace());
                    }
                })
                .to("kubernetes-job://kubernetes.default.svc?operation=createJob") // Crea il Job
                .log("Job creato con successo: ${body}");
    }
}

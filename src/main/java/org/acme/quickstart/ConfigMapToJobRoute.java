package org.acme.quickstart;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.kubernetes.client.utils.Serialization;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.kubernetes.KubernetesConstants;

import java.util.Map;

public class ConfigMapToJobRoute extends RouteBuilder {

    @Override
    public void configure() throws Exception {

        from("timer:configMapTimer?period=10000")
                .setHeader(KubernetesConstants.KUBERNETES_NAMESPACE_NAME, constant("camel-rotta"))
                .setHeader(KubernetesConstants.KUBERNETES_CONFIGMAP_NAME, constant("job-config"))
                .to("kubernetes-config-maps://kubernetes.default.svc?operation=getConfigMap")
                .log("Contenuto della ConfigMap: ${body}")
                .process(exchange -> {
                    ConfigMap configMap = exchange.getMessage().getBody(ConfigMap.class);
                    if (configMap != null && configMap.getData() != null) {
                        String jobYaml = configMap.getData().get("job-definition");
                        if (jobYaml != null) {
                            try (KubernetesClient client = new KubernetesClientBuilder().build()) {
                                Job job = Serialization.unmarshal(jobYaml, Job.class);
                                job.getMetadata().setName("camel-job-" + (int) (Math.random() * 9000 + 1000));
                                job.getMetadata().setNamespace("camel-rotta");
                                client.batch().v1().jobs().inNamespace("camel-rotta").create(job);
                                exchange.getMessage().setBody("Job creato con successo: " + job.getMetadata().getName());
                            } catch (Exception e) {
                                throw new RuntimeException("Errore durante la creazione del Job", e);
                            }
                        } else {
                            throw new RuntimeException("La chiave 'job-definition' non è presente nella ConfigMap");
                        }
                    } else {
                        throw new RuntimeException("ConfigMap non trovato o dati mancanti");
                    }
                })
                .log("${body}");
    }
}

package org.acme.quickstart;

import io.fabric8.kubernetes.api.model.ConfigMap;
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

                // Processa il contenuto della ConfigMap
                .process(exchange -> {
                    // Converti il body in un oggetto ConfigMap
                    ConfigMap configMap = exchange.getMessage().getBody(ConfigMap.class);
                    if (configMap != null && configMap.getData() != null) {
                        String jobDefinition = configMap.getData().get("job-definition");
                        if (jobDefinition != null) {
                            exchange.getMessage().setBody(jobDefinition);
                        } else {
                            throw new RuntimeException("La chiave 'job-definition' non è presente nella ConfigMap");
                        }
                    } else {
                        throw new RuntimeException("ConfigMap data non trovato o formato non valido");
                    }
                })
                .log("Job definition extracted: ${body}")

                // Crea il Job su OpenShift
                .to("kubernetes-job://kubernetes.default.svc?operation=createJob")
                .log("Job created successfully!");

    }
    }


package org.acme.quickstart;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.kubernetes.KubernetesConstants;

import java.util.Map;

public class ConfigMapToJobRoute extends RouteBuilder {

    @Override
    public void configure() throws Exception {

        from("timer:configMapTimer?period=10000")
                .log("Fetching ConfigMap...")

                // Legge la ConfigMap dal namespace
                .setHeader("CamelKubernetesNamespaceName", constant("camel-route-job"))
                .setHeader("CamelKubernetesConfigMapName", constant("job-config"))
                .to("kubernetes-config-maps://kubernetes.default.svc")
                .log("ConfigMap fetched: ${body}")

                // Processa il corpo senza usare `simple`
                .process(exchange -> {
                    // Ottieni il corpo della risposta come mappa
                    Map<String, Object> body = exchange.getMessage().getBody(Map.class);
                    if (body != null && body.containsKey("data")) {
                        Map<String, String> data = (Map<String, String>) body.get("data");
                        String jobDefinition = data.get("job-definition");
                        exchange.getMessage().setBody(jobDefinition);
                    } else {
                        throw new RuntimeException("ConfigMap data not found or invalid format");
                    }
                })
                .log("Job definition extracted: ${body}")

                // Crea il Job su OpenShift
                .setHeader("CamelKubernetesNamespaceName", constant("camel-route-job"))
                .to("kubernetes-job://kubernetes.default.svc?operation=createJob")
                .log("Job created successfully!");
    }
    }


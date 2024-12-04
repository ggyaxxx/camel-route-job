package org.acme.quickstart;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.kubernetes.KubernetesConstants;

import java.util.Map;

public class ConfigMapToJobRoute extends RouteBuilder {

    @Override
    public void configure() throws Exception {

        from("timer:configMapTimer?period=10000")
                .setHeader(KubernetesConstants.KUBERNETES_NAMESPACE_NAME, constant("camel-route-job"))
                .setHeader(KubernetesConstants.KUBERNETES_CONFIGMAP_NAME, constant("job-config"))
                .to("kubernetes-config-maps://kubernetes.default.svc?operation=getConfigMap")
                .log("Contenuto della ConfigMap: ${body}");

                // Processa il contenuto della ConfigMap
/*                .process(exchange -> {
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
                .to("kubernetes-job://kubernetes.default.svc?operation=createJob")
                .log("Job created successfully!");*/
    }
    }


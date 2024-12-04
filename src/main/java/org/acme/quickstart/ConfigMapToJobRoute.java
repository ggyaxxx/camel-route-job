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
                .process(exchange -> {
                    Map<String, Object> body = exchange.getMessage().getBody(Map.class);
                    if (body != null && body.containsKey("data")) {
                        Map<String, String> data = (Map<String, String>) body.get("data");
                        String jobDefinition = data.get("job-definition");
                        exchange.getMessage().setBody(jobDefinition);
                        String jobName = "nome-del-job";
                        exchange.getMessage().setHeader(KubernetesConstants.KUBERNETES_JOB_NAME, jobName);
                    } else {
                        throw new RuntimeException("ConfigMap data not found or invalid format");
                    }
                })
                .log("Definizione del Job estratta: ${body}")
                .to("kubernetes-job://kubernetes.default.svc?operation=createJob")
                .log("Job creato con successo!");


    }
    }


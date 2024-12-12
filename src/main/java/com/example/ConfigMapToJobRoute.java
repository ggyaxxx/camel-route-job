package com.example;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.kubernetes.KubernetesConstants;
import org.springframework.stereotype.Component;

@Component
public class ConfigMapToJobRoute extends RouteBuilder {

    @Override
    public void configure() throws Exception {
        from("timer:configMapTimer?period=10000")
                // Ottieni la ConfigMap
                .setHeader(KubernetesConstants.KUBERNETES_NAMESPACE_NAME, constant("camel-rotta"))
                .setHeader(KubernetesConstants.KUBERNETES_CONFIGMAP_NAME, constant("job-config"))
                .to("kubernetes-config-maps://kubernetes.default.svc?operation=getConfigMap")
                .log("Contenuto della ConfigMap: ${body}")
                .process(exchange -> {
                    var configMap = exchange.getMessage().getBody(io.fabric8.kubernetes.api.model.ConfigMap.class);
                    if (configMap != null && configMap.getData() != null) {
                        String jobYaml = configMap.getData().get("job-definition");
                        if (jobYaml != null) {
                            exchange.getMessage().setBody(jobYaml);
                        } else {
                            throw new RuntimeException("La chiave 'job-definition' non è presente nella ConfigMap");
                        }
                    } else {
                        throw new RuntimeException("ConfigMap non trovata o dati mancanti");
                    }
                })
                .setHeader(KubernetesConstants.KUBERNETES_NAMESPACE_NAME, constant("camel-rotta"))
                .setHeader(KubernetesConstants.KUBERNETES_JOB_NAME, simple("camel-job-${exchangeId}"))
                .to("kubernetes-job://kubernetes.default.svc?operation=createJob")
                .log("Job creato con successo: ${header.CamelKubernetesJobName}");
    }
}

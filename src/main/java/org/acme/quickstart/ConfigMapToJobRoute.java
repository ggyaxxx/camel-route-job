package org.acme.quickstart;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.kubernetes.KubernetesConstants;

public class ConfigMapToJobRoute extends RouteBuilder {

    @Override
    public void configure() throws Exception {

        from("timer:configMapTimer?period=10000") // Esegue ogni 10 secondi
                .log("Fetching ConfigMap...")

                // Legge la ConfigMap dal namespace
                .setHeader(KubernetesConstants.KUBERNETES_NAMESPACE_NAME, constant("camel-route-job"))
                .setHeader(KubernetesConstants.KUBERNETES_CONFIGMAP_NAME, constant("job-config"))
                .to("kubernetes-config-maps://kubernetes.default.svc")
                .log("ConfigMap fetched: ${body}")

                // Estrai la definizione del Job dalla ConfigMap
                .setBody(simple("${body.data['job-definition']}"))
                .log("Job definition extracted: ${body}")

                // Invia la definizione del Job all'endpoint Kubernetes
                .setHeader(KubernetesConstants.KUBERNETES_NAMESPACE_NAME, constant("camel-rotta"))
                .to("kubernetes-job://kubernetes.default.svc?operation=createJob")
                .log("Job created successfully!");
    }
}

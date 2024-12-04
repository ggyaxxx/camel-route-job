package org.acme.quickstart;

import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.api.model.batch.v1.JobSpec;
import io.fabric8.kubernetes.client.utils.Serialization;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.kubernetes.KubernetesConstants;

import java.util.List;
import java.util.Map;
import java.util.Random;

public class ConfigMapToJobRoute extends RouteBuilder {

    @Override
    public void configure() throws Exception {

        from("timer:configMapTimer?period=10000")
                .setHeader(KubernetesConstants.KUBERNETES_NAMESPACE_NAME, constant("camel-rotta"))
                .setHeader(KubernetesConstants.KUBERNETES_CONFIGMAP_NAME, constant("job-config"))
                .to("kubernetes-config-maps://kubernetes.default.svc?operation=getConfigMap")
                .log("Contenuto della ConfigMap: ${body}")
                .process(exchange -> {
                    // Lascio comunque questo blocchetto di codice per mostrare come recuperare la ConfigMap

                    ConfigMap configMap = exchange.getMessage().getBody(ConfigMap.class);
                    if (configMap != null && configMap.getData() != null) {
                        String jobName = "camel-job-" + (int)(Math.random() * 9000 + 1000);
                        exchange.getMessage().setHeader(KubernetesConstants.KUBERNETES_JOB_NAME, jobName);
                        exchange.getMessage().setHeader(KubernetesConstants.KUBERNETES_NAMESPACE_NAME, "camel-rotta");
                        exchange.getMessage().setHeader(KubernetesConstants.KUBERNETES_JOB_LABELS, Map.of("app", "camel-job"));

                        JobSpec jobSpec = generateJobSpec();
                        exchange.getMessage().setHeader(KubernetesConstants.KUBERNETES_JOB_SPEC, jobSpec);
                    } else {
                        throw new RuntimeException("ConfigMap data non trovato o formato non valido");
                    }
                })
                .to("kubernetes-job://kubernetes.default.svc?operation=createJob")
                .log("Job creato con successo!");


    }

    /* LA ROTTA FUNZIONA SOLO CREANDO IL JOB CON L'UTILIZZO DELL'OGGETTO JOBSPEC. Se si vuole utilizare la ConfigMap
       allora si dovrà per forza parsarla ed estrarre i dati.
     */


    private JobSpec generateJobSpec() {
        JobSpec jobSpec = new JobSpec();

        // Configura lo `PodTemplateSpec`
        PodTemplateSpec podTemplateSpec = new PodTemplateSpec();
        PodSpec podSpec = new PodSpec();
        podSpec.setRestartPolicy("Never");

        Container container = new Container();
        container.setName("print-time");
        container.setImage("registry.access.redhat.com/ubi8/ubi-minimal:latest");
        container.setCommand(List.of("/bin/sh", "-c"));
        container.setArgs(List.of("echo 'Current time: $(date)'"));
        podSpec.setContainers(List.of(container));

        ObjectMeta metadata = new ObjectMeta();
        metadata.setLabels(Map.of("app", "camel-job"));
        podTemplateSpec.setMetadata(metadata);
        podTemplateSpec.setSpec(podSpec);

        jobSpec.setTemplate(podTemplateSpec);
        jobSpec.setBackoffLimit(4);

        return jobSpec;
    }

}


package org.acme.quickstart;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.client.utils.Serialization;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.kubernetes.KubernetesConstants;

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
                    ConfigMap configMap = exchange.getMessage().getBody(ConfigMap.class);
                    if (configMap != null && configMap.getData() != null) {
                        String jobDefinition = configMap.getData().get("job-definition");
                        if (jobDefinition != null) {
                            // Deserializza la definizione del Job YAML in un oggetto Job
                            Job job = Serialization.unmarshal(jobDefinition, Job.class);
                            // Genera un numero casuale di 4 cifre
                            int randomNum = new Random().nextInt(9000) + 1000;
                            // Imposta il nome del Job con il numero casuale
                            String jobName = job.getMetadata().getName() + "-" + randomNum;
                            job.getMetadata().setName(jobName);
                            // Imposta l'header con il nome del Job
                            exchange.getMessage().setHeader(KubernetesConstants.KUBERNETES_JOB_NAME, jobName);
                            // Imposta il corpo del messaggio come l'oggetto Job
                            exchange.getMessage().setBody(job);
                        } else {
                            throw new RuntimeException("La chiave 'job-definition' non è presente nella ConfigMap");
                        }
                    } else {
                        throw new RuntimeException("ConfigMap data non trovato o formato non valido");
                    }
                })
                .log("Job definition extracted: ${body}")
                .to("kubernetes-job://kubernetes.default.svc?operation=createJob")
                .log("Job creato con successo!");


    }
    }


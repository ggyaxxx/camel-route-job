# S2I

As a first step, create the configmap cr in the crds/ folder. The namespace ~camel-rotta~ is hardwired in the code, so use that one.

 # Strategia documentata

https://docs.redhat.com/en/documentation/red_hat_build_of_quarkus/3.8/html-single/deploying_your_red_hat_build_of_quarkus_applications_to_openshift_container_platform/index#assembly_quarkus-openshift_quarkus-openshift

$ oc new-app registry.access.redhat.com/ubi8/openjdk-17~https://github.com/ggyaxxx/camel-route-job.git#kubernetes-client --name=camel-rotta

The above command creates a buildconfig, if there is a code change restart the build and update the image in the deployment file



$ oc start-build camel-rotta --follow


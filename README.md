
* As a first step, create the configmap cr in the crds/ folder. The namespace camel-rotta is hardwired in the code, so use that one.
* Create the sa, role and rolebinding present in the crds/folder



# Strategia documentata

* Create the deployment:

        $ oc new-app registry.access.redhat.com/ubi8/openjdk-17~https://github.com/ggyaxxx/camel-route-job.git#kubernetes-client-sp --name=camel-rotta

The above command creates a buildconfig, if there is a code change restart the build and update the image in the deployment file **$ oc start-build camel-rotta --follow**

* Assign the sa to the deployment:

        $ oc set sa deployment/camel-rotta camel-job-sa 
  * Assign the storage

          $ oc set volumes deployment/camel-rotta --mount-path /tmp/shared --add --claim-name shared-pvc --name shared-volume

* (OPTIONAL) Assign scc privileged to sa for hostpath mount

       $ oc adm policy add-scc-to-user privileged -z camel-job-sa -n camel-rotta


* (MANDATORY) if creating an nfs provider

      $ oc adm policy add-scc-to-user hostmount-anyuid -z nfs-client-provisioner -n nfs-provisioner


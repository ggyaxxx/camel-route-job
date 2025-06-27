for i in {1..10}; do
  SUFFIX=$(date +%s%N | cut -b1-13)
  echo "Creazione Job $i con SUFFIX=$SUFFIX"
  SUFFIX=$SUFFIX envsubst < only-the-job.yaml | oc create -f - &
done

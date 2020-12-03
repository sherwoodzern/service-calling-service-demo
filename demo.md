# Demo Instructions

## Ahead of time
*  Download and set up the Helidon CLI.
*  Create, build, and run the upcase app.
*  Start Prometheus to monitor our microservice's metrics.

`~/prometheus.yml`
```yaml
scrape_configs:
  - job_name: 'helidon'

    metrics_path: '/metrics/'
    static_configs:
      - targets: ['localhost:8080']
```
Launch:
```lang-bash
docker run \
    -p 9090:9090 \
    -v ~/prometheus.yml:/etc/prometheus/prometheus.yml \
    --name prom \
    --rm \
    prom/prometheus
```

The following demo was part of a session on using Eclipse MicroProfile as implemented in 
Oracle's Project Helidon, presented at The Developer's Conference, Sao Paolo, Brazil, August, 2020.

 
# During the demo

## Overview

[title slide]

This demo illustrates how to use Oracle's Project Helidon, an implementation of Eclipse 
MicroProfile, to generate a simple microservice and then enhance the microservice to illustrate 
several of the MicroProfile technologies.  

[demo slide]
 
We will write a simple microservice that returns a friendly greeting to the client.

It will use a second microservice, up-case which simply upper-cases a path parameter. I
 already have it running and here is how it works:
```lang-bash

curl --silent http://localhost:8084/upcase/Hello | jq

```

Prometheus is already running in a Docker container to monitor the metrics for our microservice. 

(show Prom, targets, down) 
 
         Using the MicroProfile REST client API we will have our microservice invoke another service
          -- upcase -- which upper-cases the input parameter.
         
         And finally we will add app-specific readiness and liveness health checks to our microservice.
          
          
I have also already downloaded and installed the Helidon CLI. See https://helidon.io/docs/v2/#/mp/cli/01_cli
 Here is how you do it:
```lang-bash


# curl -O https://helidon.io/cli/latest/darwin/helidon
# chmod +x ./helidon
# sudo mv ./helidon /usr/local/bin/

```

Now here is what we'll do _during_ the demo:
1. Use the Helidon command line to create and then continuously build and run the simple
 greeting service, and then access the app to see what it does.
1. Add an application-specific metric and use Prometheus to monitor it and the metrics Helidon
 provides automatically.
1. Change our greeting microservice so it uses the MicroProfile REST Client API to access the
  up-case microserviceservice.
1. Add custom readiness and liveness health checks.


## Create the initial microservice and try it

1. We generate the project using the Helidon CLI: (use `greeting` for package, name, artifact ID)
    ```lang-bash
    helidon init
    ```

1. Let's open the IDE and take a quick look at the generated endpoints. While it's doing that...

1. We start the Helidon CLI dev loop which automatically detects changes in our project and
 rebuilds and restarts our service:
    ```lang-bash
    cd mp-quickstart
    helidon dev
    ```
1. At this point, the greeting microservice is running. Before we make any changes to it:

   1. We'll access the microservice briefly to see it working.

      ```lang-bash
      curl --silent http://localhost:8080/greet | jq
      curl --silent http://localhost:8080/greet/Tim | jq
      ```
   1. We'll use Prometheus to see that our microservice is running.
    
   1. Show metrics and health replies.

      ```lang-bash
      curl --silent -H "Accept: application/json" http://localhost:8080/metrics | jq
      curl --silent http://localhost:8080/health/ready | jq
      curl --silent http://localhost:8080/health/live | jq
      ```
      
   1. Show OpenAPI document briefly.

      ```lang-bash
      curl http://localhost:8080/openapi
      ```
        
## Add app metric     
     
Now we will add a simple counter for the two `GET` methods.

1. We simply add a single annotation to the two `GET` methods.

   ```java
   @Counted(name="get", absolute = true, reusable = true)
   ```

1. Show the dev loop’s response to the changes.
1. Access Prometheus to see the new metric.
1. Access the greeting endpoint using curl and see the change in Prometheus.

## Add access to up-case service using MP REST Client
Three parts:
1. Add an interface to `greeting` representing the `upcase` service API.
2. Add an app-scoped bean to access the other service.
3. Change the existing greeting code to use that bean. 

Steps:
1. Add UpcaseService interface defining the upcase service for our client.

   ```java
   package greeting;
          
   import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
          
   import javax.json.JsonObject;
   import javax.ws.rs.GET;
   import javax.ws.rs.Path;
   import javax.ws.rs.PathParam;
   import javax.ws.rs.Produces;
   import javax.ws.rs.core.MediaType;
          
   @RegisterRestClient(baseUri = "http://localhost:8084/upcase")
   public interface UpcaseService {
          
       @GET
       @Path("/{value}")
       @Produces(MediaType.APPLICATION_JSON)
       public JsonObject upcase(@PathParam("value") String value);
   }
   ```
          
1. Add an app-scoped bean to access the upcase microservice.

   ```java
   package greeting;
          
   import org.eclipse.microprofile.rest.client.inject.RestClient;
          
   import javax.enterprise.context.ApplicationScoped;
   import javax.inject.Inject;
         
   @ApplicationScoped
   public class UpcaseAccess {
          
       @Inject
       @RestClient
       UpcaseService upcaseService;
          
       public String getUpcase(String value) {
            return upcaseService.upcase(value).getString("value");
       }
   }
   ```
     
1. Revise the createResponse method to invoke the up-case microservice:
   1. Add private field holding a ref to the `UpcaseAccess` class.
         
      ```java   
       private final UpcaseAccess upcaseAccess;
      ```
                
   1. Change injected constructor to accept and set an upcaseAccess.
      
      ```java   
      @Inject
      public GreetResource(GreetingProvider greetingConfig, UpcaseAccess upcaseAccess) {
          this.greetingProvider = greetingConfig;
          this.upcaseAccess = upcaseAccess;
      }
      ```
                
   1. Change `createResponse` method to invoke `upcaseAccess.getUpcase(who)`:
      
      ```java   
      String msg = String.format("%s %s!", greetingProvider.getMessage(), upcaseAccess.getUpcase(who));
      ```
      
   1. Access the greet microservice and notice that the returned values are up-cased.
      ```lang-bash
      curl --silent http://localhost:8080/greet | jq
      curl --silent http://localhost:8080/greet/Tim | jq
      ```

1. Add a liveness and readiness health check classes.

"Liveness" simply means the microservice is up. A microservice reporting that it is not alive should
 typically be restarted (e.g., by Kubernetes). 

"Readiness" means the service is capable of doing the work expected of it. For example,
 Kubernetes will not direct requests to a microservice reporting that is it not ready.
   
   1. Add `GreetLivenessCheck` class to check for at least two processors.
      ```java
      package greeting;
      
      import org.eclipse.microprofile.health.HealthCheck;
      import org.eclipse.microprofile.health.HealthCheckResponse;
      import org.eclipse.microprofile.health.Liveness;
      
      import javax.enterprise.context.ApplicationScoped;
      import java.util.Map;
      import java.util.Optional;
      
      @Liveness
      @ApplicationScoped
      public class GreetLivenessCheck implements HealthCheck {
      
          private Runtime runtime = Runtime.getRuntime();
      
          @Override
          public HealthCheckResponse call() {
              return HealthCheckResponse.named("GreetLivenessCheck")
                      .state(runtime.availableProcessors() > 1)
                      .withData("availableProcessors", runtime.availableProcessors())
                      .build();
          }
      }
      ```
           
   1. Create `GreetReadinessCheck` class that checks to be sure the upcase service response in no
    more than 200 ms:
   
      ```java
      package greeting;
                 
      import org.eclipse.microprofile.health.HealthCheck;
      import org.eclipse.microprofile.health.HealthCheckResponse;
      import org.eclipse.microprofile.health.Readiness;
      
      import javax.enterprise.context.ApplicationScoped;
      import javax.inject.Inject;
      
      @Readiness
      @ApplicationScoped
      public class GreetReadinessCheck implements HealthCheck {
      
          @Inject
          UpcaseAccess upcaseAccess;
      
          @Override
          public HealthCheckResponse call() {
              long start = System.currentTimeMillis();
              upcaseAccess.getUpcase("ping");
              long elapsedTime = System.currentTimeMillis() - start;
              return HealthCheckResponse.named("GreetLivenessCheck")
                      .state(elapsedTime < 200)
                      .withData("elapsedMS", elapsedTime)
                      .build();
          }
      }
      ```
          
1. Show the updated microservice’s behavior:
   1. Access `/health/live`.
      ```lang-bash
      curl --silent http://localhost:8080/health/live | jq
      ```
      
      Note the overall value and the liveness check we added.
   1. Access `/health/ready`.
      ```lang-bash   
      curl --silent http://localhost:8080/health/ready | jq
      ```

1. Change the delay in the up-case microservice (to force the custom greeting liveness check to
 report `DOWN`):
   ```lang-bash
   curl -i -X PUT -H "Content-Type: application/json"  -d '{ "delay" : 500}' http://localhost:8084/upcase
   ```

1. Show the microservice’s health:
   1. Access /health/live. Note it’s DOWN.
   ```lang-bash
   curl --silent http://localhost:8080/health/live | jq
   ```

1. Show readiness.
   1. Access /health/ready. Note there are no checks.
   ```lang-bash
   curl --silent http://localhost:8080/health/ready | jq
   ```
   
# Wrap-up

That is our demo!

To wrap up, in just a few minutes we have:
 
1. used Helidon to generate a simple microservice,
2. added a custom metric to our microservice and used industry-standard tools to 
 monitor it,
3. invoked an entirely separate microservice from inside our microserivce, and
4. added custom health checks tailored to the details of our microservice.

Certainly this is a very simple example, but you can see how easily you can create and enhance
 your own microservices using Helidon MP and MicroProfile.
        
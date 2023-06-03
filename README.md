# POC of Layered JARs in Docker using Spring Boot

## Description
### Purpose
The following idea, is to assess, compare and contrast against various JAR build types. This is because the increasing use of DevOps processes and micro service architecture has raised the importance of the footprint of the application. This means the smaller the application size the better it is for the DevOps process, it will take less time to build and optimize the upload time.


### What are the types of qualifiers?

![Qualifier](https://i.imgur.com/6TLMT8T.png "Qualifier")

#### Fat
- Includes everything needed to run the whole app on a standard Java Runtime environment
- Includes dependencies that usually don’t change between releases

```text
./fat-jar/
├── BOOT-INF
│   ├── classes
│   │   └── com
│   │       └── example
│   │           └── demo
│   │               ├── BoosterApplication.class
│   │               └── service
│   │                   └── GreetingProperties.class
│   ├── classpath.idx
│   ├── layers.idx
│   └── lib
│       └── ant-1.9.4.jar
├── META-INF
│   ├── MANIFEST.MF
│   └── maven
│       └── com.example.demo
│           └── greeting-spring-boot-fat
│               ├── pom.properties
│               └── pom.xml
└── org
    └── springframework
        └── boot
            └── loader
                ├── ClassPathIndexFile.class
                ├── archive
                │   └── Archive$Entry.class
                ├── data
                │   └── RandomAccessData.class
                ├── jar
                │   └── AbstractJarFile$JarFileType.class
                └── jarmode
                    └── JarMode.class
```

#### Skinny
- This contains only the code written by the developer and nothing else. 

```text
skinny-jar/
├── META-INF
│   ├── MANIFEST.MF
│   └── maven
│       └── com.test
│           └── weight-skinny
│               ├── pom.properties
│               └── pom.xml
└── com
    └── test
        └── rest
            └── RestApplication.class
```

#### Thin
- Includes the code
- Includes associated dependencies with the code

```text
thin-jar/
├── META-INF
│   ├── MANIFEST.MF
│   └── maven
│       └── com.example.demo
│           └── greeting-spring-boot-thin
│               ├── pom.properties
│               └── pom.xml
├── com
│   └── example
│       └── demo
│           ├── BoosterApplication.class
│           └── service
│               ├── GreetingController.class
│               └── GreetingProperties.class
├── lib
└── org
    └── springframework
        └── boot
            └── loader
                └── wrapper
                    └── ThinJarWrapper.class
```

#### Hollow
- Includes only the runtime components
- Does not include your application code

```text
hollow-jar/
├── META-INF
│   ├── MANIFEST.MF
│   └── wildfly-swarm-manifest.yaml
├── XPP3-LICENSE.txt
├── __redirected
│   └── __XPathFactory.class
├── m2repo
│   ├── ch
│   │   └── qos
│   │       └── cal10n
│   │           └── cal10n-api
│   │               └── 0.8.1
│   │                   └── cal10n-api-0.8.1.jar
│   ├── com
│   │   └── eclipsesource
│   │       └── minimal-json
│   │           └── minimal-json
│   │               └── 0.9.4
│   │                   └── minimal-json-0.9.4.jar
│   └── xerces
│       └── xercesImpl
│           └── 2.11.0.SP5
│               └── xercesImpl-2.11.0.SP5.jar
├── org
│   ├── wildfly
│   │   └── swarm
│   │       └── bootstrap
│   │           ├── Main.class
│   │           ├── MainInvoker.class
│   │           └── env
│   │               └── WildFlySwarmManifest.class
│   └── yaml
│       └── snakeyaml
│           ├── DumperOptions$FlowStyle.class
│           ├── tokens
│           │   ├── AliasToken.class
│           └── util
│               └── UriEncoder.class
└── schema
    └── module-1_5.xsd
```

## Spring Boot with Docker
### Fat JAR
In the common way when Spring Boot App integrates with Docker, usually the Project consist of a “Dockerfile” and it fetches an image from Docker Hub and creates an image injecting the fat JAR file.

```dockerfile
FROM adoptopenjdk:11-jre-hotspot
WORKDIR application
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} application.jar
ENTRYPOINT java -jar application.jar
```

Example of a Dockerfile containing common configuration

This can be built in to a Docker image and push to the Docker Hub using docker commands as in the usual manner.


## The Problem: The Dependencies in the Fat JAR

![Fat JAR uploads all the things bundled together with each release](https://i.imgur.com/MUZS95v.png "Fat JAR uploads all the things bundled together with each release")

![Inside of a Fat JAR. It consist of all the Dependencies, Resources and Code](https://i.imgur.com/4qyt6Q5.png "Inside of a Fat JAR. It consist of all the Dependencies, Resources and Code")

- With Fat JAR approach, each new release uploads the Fat JAR.
- Fat JAR contains all the Dependencies, Resources and Code.
- But all the dependencies or resources might not be changed between releases.
- Even if these are not changing it will be uploaded again and again.
- Problem is Uploading a Fat JAR is a waste of spaces, bandwidth and time.

## The Solution: Different Layers for Dependencies, Resources, and Code
### Layered JAR

![Splitting Dependencies, Resources and Code to separate layers](https://i.imgur.com/QQvgWcZ.png "Splitting Dependencies, Resources and Code to separate layers")
![Spring Boot JAR is split across different layers](https://i.imgur.com/iTNw9j2.png "Spring Boot JAR is split across different layers")
![Using Docker Layered architecture along with Spring Boot Layered JAR](https://i.imgur.com/J4xXY9p.png "Using Docker Layered architecture along with Spring Boot Layered JAR")

- With the Spring Boot 2.3 release, it brings with it some interesting new features that can help to package up Spring Boot application into Docker images.
- This release allows the decomposition of the application into different layers.
- As per the above images, the application has its own layer.
- When modifying the source code, only the independent application layer is rebuilt. The loader and the dependencies remain cached.
- Docker splits the content of the Docker container into layers.
- Docker containers consist of a base image and additional layers.
- Once the layers are built, they will remain cached and subsequent rebuilds are generated much faster.

Layered JAR approach, Spring Boot generates the fat JAR file as usual but with the capability of extracting it into different layers as folders each containing code, dependencies and etc.When it integrates with Docker, it calculates the hash for each layer and only rebuild the layers that has changed, in the subsequent generations. Default Spring Boot Layered JAR has 4 layers,

- dependencies (for regular released dependencies)
- snapshot-dependencies (for snapshot dependencies)
- resources (for static resources)
- application (for application classes and resources)

## Design
### Design diagram to test the approach

![Design diagram to test the approach](https://i.imgur.com/AqlDhvF.png "Design diagram to test the approach")

#### Initial build comparison
This is the time analysis of initial Docker image build and upload to the Docker Hub

#### Subsequent code generation
This is the time analysis of subsequent code modification and upload to the Docker Hub for both Fat JAR approach and Layered approach

#### Subsequently adding dependencies
This is the time analysis of subsequently adding dependencies and upload to the Docker Hub for both Fat JAR approach and Layered approach (for both default Layered JAR and Custom Layered JAR)

## Creating Efficient Docker Images with Spring Boot
### Layer configuration
As the first step spring boot needs to be updated to a 2.3 version in the pom.xml

```xml
<parent>  
    <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-parent</artifactId>
      <version>2.3.12.RELEASE</version>
      <relativePath/>
</parent>
```

Then need to add the configuration to enable layers in the spring-boot-maven-plugin in the pom.xml. From Spring Boot 2.4 snapshots, layering is enabled by default

```xml
<build>  
    <plugins>
      <plugin>
         <groupId>org.springframework.boot</groupId>
         <artifactId>spring-boot-maven-plugin</artifactId>
         <version>${project.parent.version}</version>
         <configuration>
            <layers>
               <enabled>true</enabled>
            </layers>
         </configuration>
      </plugin>
   </plugins>
</build>
```

To simply test if this layering works,

- Run mvn clean package
- Then go inside the target directory in the project
- Execute java -Djarmode=layertools -jar application.jar list
- Previous command displays list of layers that are available
- Executing java -Djarmode=layertools -jar application.jar extract will result in creating these layers as folder in the target folder

### Docker file configuration
In order to integrate the Layered JAR project with Docker, all the layers should be included in the “Dockerfile” as below

```dockerfile
FROM adoptopenjdk:11-jre-hotspot as builder
WORKDIR application
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} application.jar
RUN java -Djarmode=layertools -jar application.jar extract
 
FROM adoptopenjdk:11-jre-hotspot
WORKDIR application
COPY --from=builder application/dependencies/ ./
COPY --from=builder application/snapshot-dependencies/ ./
COPY --from=builder application/spring-boot-loader/ ./
COPY --from=builder application/application/ ./
ENTRYPOINT ["java", "org.springframework.boot.loader.JarLauncher"]
```

### Building and Running the Application
- Execute “mvn clean package” in the project directory
- Then build the Docker image executing “docker build -t docker-jar-size-test-layered:1 .”
  - 1 after the : is the tag of the Docker image
  - at the end of the docker build command tells that Docker should look for the Dockerfile in the current directory.
- To integrate this image with repository in the Docker execute – “docker tag ”docker-jar-size-test-layered skaveesh/docker-jar-size-test-layered”. “skaveesh/docker-jar-size-test-layered” is the name of the repository which exists on the Docker Hub.
- Finally execute - “docker push skaveesh/docker-jar-size-test-layered:1” and this will push the image into the Docker Hub.

## Custom Layers

### Custom Layer Configuration
In the default configuration Spring Boot provides only 4 layers as mentioned previously. But with the custom layer configuration project can be configured with additional layers.

For example, if the project contains Spring Boot dependencies and Custom dependencies, with the default layer configuration it puts all the dependencies into a single dependency layer.

But with the custom layer configuration it is possible to split Spring Boot dependencies and Custom dependencies into two different layers

### Advantage of Custom Layering
In the default manner every time project is updated with new dependency regardless of its type. Docker will rebuild the dependency layer and upload it each time.

But in real world scenarios Custom dependencies and Spring Boot dependencies are updated in different cadence, meaning that Custom dependencies are updated more often. Every time when Custom dependency is updated there is no benefit in updating the Spring Boot dependencies. Thus, Custom and Spring Boot dependencies can be grouped into separate layers and only upload the layer which has an update, each time Docker image is built. This is more efficient and less wasteful of resources.

### Adding New Custom Layer
As first step spring-boot-maven-plugin in the pom.xml needs to be updated with layer configuration file location.

```xml
<build>  
    <plugins>
      <plugin>
         <groupId>org.springframework.boot</groupId>
         <artifactId>spring-boot-maven-plugin</artifactId>
         <version>${project.parent.version}</version>
         <configuration>
            <layers>
               <enabled>true</enabled>
               <configuration>${project.basedir}/layers.xml</configuration>
            </layers>
         </configuration>
      </plugin>
   </plugins>
</build>
```

Configuration file of for layers is a xml file as below (layer.xml) which resides in the root directory of the project. Notice that custom-dependencies has a separate layer. The <dependencies> section uses group:artifact[:version] patterns. It also provides <includeModuleDependencies /> and <excludeModuleDependencies /> elements that can be used to include or exclude local module dependencies.

```xml
<layers xmlns="http://www.springframework.org/schema/boot/layers"       
 
xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:schemaLocation="http://www.springframework.org/schema/boot/layers
              https://www.springframework.org/schema/boot/layers/layers-2.3.xsd">
 
    <application>
        <into layer="spring-boot-loader">
            <include>org/springframework/boot/loader/**</include>
        </into>
        <into layer="application" />
    </application>
    <dependencies>
        <into layer="snapshot-dependencies">
            <include>*:*:*SNAPSHOT</include>
        </into>
        <into layer="custom-dependencies">
            <include>org.projectlombok:lombok:*</include>
        </into>
        <into layer="dependencies"/>
    </dependencies>
    <layerOrder>
        <layer>dependencies</layer>
        <layer>spring-boot-loader</layer>
        <layer>snapshot-dependencies</layer>
        <layer>custom-dependencies</layer>
        <layer>application</layer>
    </layerOrder>
</layers>
```

### Docker file configuration
Dockerfile need to be updated accordingly

```dockerfile
FROM adoptopenjdk:11-jre-hotspot as builder
WORKDIR application
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} application.jar
RUN java -Djarmode=layertools -jar application.jar extract
 
FROM adoptopenjdk:11-jre-hotspot
WORKDIR application
COPY --from=builder application/dependencies/ ./
COPY --from=builder application/snapshot-dependencies/ ./
COPY --from=builder application/spring-boot-loader/ ./
COPY --from=builder application/application/ ./
COPY --from=builder application/custom-dependencies/ ./
ENTRYPOINT ["java", "org.springframework.boot.loader.JarLauncher"]
```

## Analysis
### Docker builds upload time analysis demo
#### Subsequent code generation with Fat JAR approach
https://github.com/skaveesh/poc-docker-layered-jars/assets/14146760/259d83c2-f843-4d6f-9bc6-c1015ab19f74

#### Subsequent code generation with Layered JAR approach
https://github.com/skaveesh/poc-docker-layered-jars/assets/14146760/2ff006af-384c-49ae-b7e4-ca3c46be8d5a



### Test analysis – Fat vs Layered (build size, upload size, upload time)
Upload time is calculated by wrapping the command that is pushing the Docker image to the Docker Hub with time calculating command as below.


Docker push command:

```shell
docker push skaveesh/docker-jar-size-test-layered:1
```

Docker push command with time calculations:
```shell
START=$(date +%s) && docker push skaveesh/docker-jar-size-test-layered:1 && END=$(date +%s) && echo $((END-START)) | awk '{print int($1/60)" mins "int($1%60)" sec elapsed"}'
```
#### Final analysis of the results

| Test Case                                 | JAR Type         | Upload Size | Upload Time   |
|-------------------------------------------|------------------|-------------|---------------|
| Test 1 – Initial build                    | Fat              | 97.94 MB    | 7 mins 37 sec |
| Test 2 – Initial build                    | Default Layering | 97.9 MB     | 7 mins 58 sec |
| Test 3 – Initial build                    | Custom Layering  | 97.9 MB     | 7 mins 58 sec |
| Test 4 – Subsequent code generation       | Fat              | 16.67 Mb    | 1 min 21 sec  |
| Test 5 –Subsequent code generation        | Default Layering | 20.48 Kb    | 24 sec        |
| Test 6 –Subsequent code generation        | Custom Layering  | 20.48 Kb    | 24 sec        |
| Test 7 – Subsequently adding dependencies | Fat              | 18.59 Mb    | 1 min 45 sec  |
| Test 8 – Subsequently adding dependencies | Default Layering | 18.59 Mb    | 1 min 38 sec  |
| Test 9 – Subsequently adding dependencies | Custom Layering  | 1.942 Mb    | 26 sec        |

#### Build Size and Upload Time Comparison

![Upload size analysis](https://i.imgur.com/ZVRr69q.png "Upload size analysis")
![Upload time analysis](https://i.imgur.com/AApyaZB.png "Upload time analysis")

## Pros and Cons
### Fat JAR

| Pros                                                               | Cons                                                               | 
|--------------------------------------------------------------------|--------------------------------------------------------------------|
| Not reliant on dependencies being in the destination               | Subsequent build and upload is wasteful in terms of time and space | 
| Subsequent build and upload is wasteful in terms of time and space |                                                                    | 

### Layered JAR

| Pros                                               | Cons                                        | 
|----------------------------------------------------|---------------------------------------------|
| Smaller subsequent build sizen                     | Initial build is the same as of the Fat JAR | 
| It takes less time to upload                       |                                             | 
| Dependencies are already cached in the destination |                                             |

## Outcomes
- Using Docker with Fat JAR is a resource taking and will take more time to build and upload in a subsequent deployment.
- But with Default Layered JARs, it will not take much time to build the code, but it will take more time to build the dependencies if it is changed in a subsequent build.
- But with Custom Layered JARs, since it supports any customization to the dependency layer, it will not take much time to build because dependencies are grouped into logical layers.

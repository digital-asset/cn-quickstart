# Introduction

Canton Network Application Quickstart provides a set of observability services to help developers monitor and debug
their applications.

The observability signals (logs, metrics, traces) are powered by well-known open-source projects.
The UI of Grafana has everything one needs to visualize and analyze the data with preconfigured datasources and settings
to allow seamless navigation between different signal types.

# Topology
The graph below schematically represents the flow of observability signals in the Quickstart.

```mermaid
%%{init: {'theme':'forest'}}%%
flowchart LR

%% Nodes
A("OTEL Collector"):::green
B("Prometheus<br>(metrics)"):::blue
C("Loki<br>(logs)"):::pink
D("Tempo<br>(traces)"):::purple
E("Grafana"):::orange
F("pqs")
G("backend-service")
H("frontend")
I("canton<br>(traces only)"):::dashed-border
J("postgres<br>{canton}")
K("postgres<br>{pqs}")
L("canton")

%% Groups
subgraph Apps-1
  direction RL
  H
  J
  K
  L
end

subgraph Apps-2
  direction RL
  F
  G
  I
end

subgraph Observability-Backends
  B
  C
  D
end

%% Edges
Apps-1 -- fluentd<br>(logs) --> A
Apps-1 <-. http<br>(metrics scrape) .-> A
Apps-2 -- otlp/grpc<br>(logs, metrics, traces) --> A
A -- otlp/http --> B
A -- otlp/http --> C
A -- otlp/grpc --> D
B <-. http .-> E
C <-. http .-> E
D <-. http .-> E

%% Styling
classDef dashed-border stroke:black,stroke-dasharray: 5 5
classDef green fill:#B2DFDB,stroke:#00897B,stroke-width:2px;
classDef orange fill:#FFE0B2,stroke:#FB8C00,stroke-width:2px;
classDef blue fill:#BBDEFB,stroke:#1976D2,stroke-width:2px;
classDef yellow fill:#FFF9C4,stroke:#FBC02D,stroke-width:2px;
classDef pink fill:#F8BBD0,stroke:#C2185B,stroke-width:2px;
classDef purple fill:#E1BEE7,stroke:#8E24AA,stroke-width:2px;

%% Meta
click A "https://opentelemetry.io/docs/collector/" _blank
click B "https://prometheus.io/" _blank
click C "https://grafana.com/oss/loki/" _blank
click D "https://grafana.com/oss/tempo/" _blank
click E "https://grafana.com/oss/grafana/" _blank
```

# Running

To start the observability services, set ``OBSERVABILITY_ENABLED`` to ``true`` in ``.env.local`` in the ``quickstart`` directory, and it will be started together with the application services when you run
```shell
$ make start
```

Then navigate to [http://localhost:3030](http://localhost:3030) to access the Grafana UI.

flowchart LR
%% 외부 환경 및 개발자 도구
EdgeServer["💻 Edge Server"]
Developer["👨‍💻 Developer"]
Kubectl["🖥️ kubectl"]

    %% CI/CD 파이프라인
    subgraph CICD [" "]
        direction LR
        Github["🐙 Github\n버전 관리"]
        Actions["⚙️ Github actions"]
    end

    Developer --> Github
    Github -->|"Git push"| Actions

    %% AWS Cloud 영역
    subgraph AWS_Cloud ["☁️ AWS Cloud"]
        direction TB
        EKS_Control["🟧 Amazon EKS"]
        ECR["🟧 Amazon ECR\nRegistry"]

        subgraph VPC ["☁️ VPC"]
            direction LR
            subgraph Public_Subnet ["🔓 Public subnet"]
                ALB(("🌐 ALB\nLoad Balancer"))
            end

            subgraph Private_Subnet ["🔒 Private subnet"]
                direction LR
                subgraph EKS_Cluster ["🚢 EKS_Cluster_SeRVe"]
                    direction TB
                    CorePod["🟢 Springboot\nCore pod"]
                    TeamPod["🟢 Springboot\nTeam pod"]
                    AuthPod["🟢 Springboot\nAuth pod"]
                end
                DB[/"🗄️ MariaDB"\]
            end
        end
    end

    %% 트래픽 및 네트워크 연결
    EdgeServer --> ALB
    
    ALB -->|"Route"| CorePod
    ALB -->|"Route"| TeamPod
    ALB -->|"Route"| AuthPod

    CorePod -->|"JDBC"| DB
    TeamPod -->|"JDBC"| DB
    AuthPod -->|"JDBC"| DB

    %% 배포 및 관리 흐름
    Actions -->|"Docker Image\n생성"| ECR
    ECR -->|"Pull & Deploy"| EKS_Cluster
    
    Kubectl --> EKS_Control
    EKS_Control --> EKS_Cluster
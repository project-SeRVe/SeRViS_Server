```
cd client-robot
python -m venv .venv # python SDK 필수
.venv\Scripts\activate
pip install -r requirements.txt
```


```
SeRVe/
├── src/                    # Java 서버
│   ├── main/
│   └── test/
├── client-robot/           # Python 클라이언트 (모듈)
│   ├── .venv/             
│   ├── security/
│   ├── main.py
│   ├── requirements.txt
│   └── config.py
├── build.gradle
└── .gitignore
```

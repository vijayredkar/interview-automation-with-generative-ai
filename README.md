# interview-automation-with-generative-ai
Automation to interview &amp; evaluate candidate responses. 
Application speaks the questions &amp; evaluates candidate's answer by employing Generative Ai.

Steps to launch :
1. create your Open API key from https://platform.openai.com/
2. /src/main/resources/application.properties has    openai.api.key=PASTE-YOUR-SPECIFIC-OPEN-API-KEY-HERE
3. replace "PASTE-YOUR-SPECIFIC-OPEN-API-KEY-HERE" with your Open API key
4. copy GIT speech.properties in to you machine's home directory 
5. for eg. in my case it is C:\Users\user\speech.properties  [ref. https://www.javatpoint.com/convert-text-to-speech-in-java]
6. launch  run_interview.bat   to start the application
7. On startup a new interview session will begin.
8. This application will speak to you and provide interview instructions
9. Application will ask you questions, capture your responses & finally provide the candidate evaluation.

1. Problem statement & solution approach - https://vijayredkar.medium.com/   
4. Launch microservices
   - git clone in to your 
     - <project_dir> - git clone https://github.com/vijayredkar/event-driven-platform.git
   #### core services
   - entity-mgt launch      
     - cd <project_dir>\event-driven-platform\entity-management >  mvn spring-boot:run

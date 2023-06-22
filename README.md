# interview-automation-with-generative-ai
Automation to interview &amp; evaluate candidate responses. 
Application speaks the question s &amp; evaluates candidate's answer by employing Generative Ai.

Steps to launch :
1. git clone https://github.com/vijayredkar/interview-automation-with-generative-ai.git on to your c:\
2. create your Open API key from https://platform.openai.com/
3. /src/main/resources/application.properties has    openai.api.key=PASTE-YOUR-SPECIFIC-OPEN-API-KEY-HERE.
4. replace "PASTE-YOUR-SPECIFIC-OPEN-API-KEY-HERE" with your Open API key.
5. copy speech.properties in to your machine's home directory.
6. for eg. in my case it is C:\Users\user\speech.properties  [ref. https://www.javatpoint.com/convert-text-to-speech-in-java]
7. launch  run-interview.bat   to start the application
8. on startup a new live interview session will begin.
9. this application will speak to you and provide interview instructions.
10. Application will ask you questions, capture your responses & finally provide the candidate evaluation.

11. ![1-automated-interviewer-evaluater](https://github.com/vijayredkar/interview-automation-with-generative-ai/assets/25388646/21715a66-a8aa-47aa-82f1-69220fbe876a)

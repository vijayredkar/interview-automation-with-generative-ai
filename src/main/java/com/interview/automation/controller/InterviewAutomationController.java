package com.interview.automation.controller;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.Scanner;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.RandomUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.interview.automation.model.InterviewGptRequest;
import com.interview.automation.model.InterviewGptResponse;
import com.interview.automation.voice.TextToSpeech;


@RestController
@RequestMapping(value = "/openai/v1/")
public class InterviewAutomationController 
{

    @Qualifier("openaiRestTemplate")
    @Autowired
    private RestTemplate restTemplate; //intercepts the request to this controller and add the OpenAPi key to the header

    @Value("${openai.model}")
    private String model;

    @Value("${openai.api.url}")
    private String apiUrl;
    
    @Autowired
    TextToSpeech textToSpeech;
          
    List<String> interviewConfigModesList;
    List<String> questionsFinalList;  //ensure that this is always an ArraList. Otherwise randomize fn will fail
    List<String> randomizedQnsList;   
	List<String> candidateAnswersList;
	List<String> interviewMockQnsList;
	List<String> interviewMockAnsList;
	List<String> interviewRealQnsList;
	
	ListIterator<String> qsItr;
	ListIterator<String> ansItr;
	
	boolean preTrainModelEnabled;
	boolean preInstructionsEnabled;
	boolean mockInterview;
	
	InterviewGptResponse response;
	String interviewTranscript;
	String candidateEvaluation;
	
	String prompt_context_criteria_1;
	String prompt_training_data_2;
	String prompt_gpt_complete;
	
	String interview_pre_instructions_3;
	String interview_mock_questions_4;
	String interview_mock_answers_5;
	String interview_real_questions_6;
	
	String path_base_1 = System.getProperty("user.dir") + "\\";
	String path_profile_2 = "interview_profile\\PLACE_HOLDER_PROFILE_PATH\\";
	String path_base_with_profile_3 = path_base_1 + path_profile_2 ;
	
	String interview_profile;
	String limitNumQnsToAsk;
	int limitNumQnsToAskFinal;
	
	
	@PostConstruct
    public void onStartup() throws Exception 
    {	
		interview();		
    }
	
	private void newInterviewSession() throws Exception 
	{
		//-- another interview
		System.out.println("\n\n Admin, if you wish to start a new automated interview session, please enter Y. ");
		speak("Admin, if you wish to start a new automated interview session, please enter Y. ");
		
		String replyComplete= captureInput();
		
		while(!"Y".equalsIgnoreCase(replyComplete))
		{
			System.out.println("\n\n Admin, please enter Y to start a new automated interview session. ");
			speak("Admin, please enter Y to start a new automated interview session.");
			replyComplete = captureInput();
		}
		
		interview();
	}	
	
    //@GetMapping("/interview")
    //public String interview() throws Exception 
	public void interview() throws Exception
    {   
    	initialize();
    	configureModeWithSafeDefaults();
    	preInstructions();
    	configureModesWithUserSpecified(interviewConfigModesList);
    	warnModeSelection();
    	preTrainModel();
    	startInterview();    	
    	concludeInterview();
    	startEvaluation();
    	newInterviewSession();
    }
    
    private void configureModeWithSafeDefaults() 
	{
		//--default safe config
		mockInterview = false;         // be extremely cautious
		preInstructionsEnabled = true; // change only for convenience sake
		preTrainModelEnabled = true;   //don't change this ever
		//-- default safe config
	}
    
    /* 
	 * CAUTION : Be EXTREMELY careful if/when changing the default config modes. 
	 *           interview-config-modes-0.txt
	 */
	private void configureModesWithUserSpecified(List<String> interviewConfigModesList) 
	{	
		//-- modified as  per user config in interview-config-modes-0.txt
		interviewConfigModesList.forEach(param -> {	
													 param = param.replaceAll("\\s", ""); //remove all spaces from i/p 
													 
													 if ("mockInterview=true".equals(param)) 
													 	{ mockInterview = true;}
													 
													 if ("preInstructionsEnabled=false".equals(param)) 
													 	{ mockInterview = false;}
													 
													 if (!("").equals(param.split("=")[1].trim())) //checks if user provided a value int on RHS   limitNumQnsToAsk=3
													 	{ limitNumQnsToAsk = param.split("=")[1].trim();}													 
										});
		//-- modified as  per user config in interview-config-modes-0.txt
	}

	private void setInterviewProfile(List<String> interviewConfigModesList) 
	{	
		
		//-- interview-config-modes-0.txt
		
		String profile1 =  interviewConfigModesList.stream()
				                                  .filter(param -> param.startsWith("profile")) //profile = software_engineer  -> interview_profile = software_engineer
				                                  .findFirst().get();
		
		profile1 = profile1.replaceAll("\\s", "");
		profile1 = profile1.split("=")[1];  //profile1 = software_engineer /  sixth_grader
				
		interview_profile = profile1;
	}
	
	private void preTrainModel() 
	{   
		if(preTrainModelEnabled)
		{	
			// more specific the criteria, closer is GPT evaluation to the expected response
			prompt_context_criteria_1 = extractFileContent(path_base_with_profile_3 + "interview-context-criteria-1.txt", null);
			
			// w/o pre training results of GPT show larger variance
			prompt_training_data_2 = extractFileContent(path_base_with_profile_3 + "interview-training-data-2.txt", null);
			
			prompt_gpt_complete = prompt_context_criteria_1 + prompt_training_data_2;
		}		
	}

	private void preInstructions() 
	{
		if(preInstructionsEnabled)
    	{
			System.out.println("\n\n--- preInstructions "+ interview_profile + "\n\n");
			interview_pre_instructions_3 = extractFileContent(path_base_with_profile_3 + "interview-pre-instructions-3.txt", null);
			System.out.println(interview_pre_instructions_3);
			speak(interview_pre_instructions_3);
			System.out.println("\n\n");
    	}
	}

	private void initialize() 
	{	
		interviewConfigModesList = new ArrayList<String>();
		interviewMockQnsList = new ArrayList<String>(); //ensure that this is always an ArraList. Otherwise randomize fn will fail
		interviewMockAnsList = new ArrayList<String>();
		interviewRealQnsList = new ArrayList<String>();
		
		extractFileContent(path_base_1 + "interview-config-modes-0.txt", interviewConfigModesList);
		setInterviewProfile(interviewConfigModesList);
			
		
		//for test interviews only
		extractFileContent(path_base_with_profile_3 + "interview-mock-questions-4.txt", interviewMockQnsList);
		extractFileContent(path_base_with_profile_3 + "interview-mock-answers-5.txt", interviewMockAnsList);
		
		//for real interviews
		extractFileContent(path_base_with_profile_3 + "interview-real-questions-6.txt", interviewRealQnsList);				
	}

	private void warnModeSelection() 
	{
		String replyComplete = "";
		while(mockInterview && !"Y".equalsIgnoreCase(replyComplete)) //mock interview but user has not double confirmed with Y 
		{			
			System.out.println("\n\n You have chosen to launch this interview in mock mode.");
			//System.out.println("\n\n This is a test mode to verify that the application works as expected against pre defined mock question and answers ");
			System.out.println("\n\n Please enter Y to confirm your selection");
			
			speak("You have chosen to launch this interview in mock mode.");
			//speak("This is a test mode to verify that the application works as expected against pre defined mock question and answers ");
			speak("Please enter Y to confirm your selection");
			
					
		    replyComplete = captureInput();		    
		    
		    if(!"Y".equalsIgnoreCase(replyComplete))
			{
				System.out.println("\n\n You did not confirm with a Y. You entered : " + replyComplete);
				System.out.println("Either confirm to continue to in mock mode by entering Y or set the mockInterview mode to false in the system configuration.");
				
				speak("You did not confirm with a Y ");
				speak("Either confirm to continue to in mock mode by entering Y or set the mockInterview mode to false in the system configuration.");
				
			} //end while
		    
		    System.out.println("\n\n You confirmed to launch this interview in Mock mode.");
		    speak("You confirmed to launch this interview in Mock mode");
		} //end while
				
	    configureModesWithUserSpecified(interviewConfigModesList);
	    setInterviewProfile(interviewConfigModesList);
	}	
	
	public void setModes(String userSpecifiedMode)
	{
		String[] systemParameter = userSpecifiedMode.split("=");
		
		if("pretrainEnabled=true".trim().equals(userSpecifiedMode))
		{
			preTrainModelEnabled = true;	
		}
		else
		{
			preTrainModelEnabled = false;
		}
	}
	
	private void startInterview() throws InterruptedException 
	{
		System.out.println("-------------------> Interview Starts \n\n");
		
		setFinalQuestionsList();		
    	setInterviewLength(questionsFinalList);
    	conductInterview();
    	
    	System.out.println("-------------------> Interview Finished \n\n");
	}

	private void conductInterview() throws InterruptedException 
	{
		int numQnsAskedAlready = 0;
    	while(qsItr.hasNext() && (numQnsAskedAlready < limitNumQnsToAskFinal)) // iterate until the final agreed length limit  
    	{
    		askQuestion(qsItr.next());
    		getAnswer(mockInterview);
    		
    		numQnsAskedAlready = numQnsAskedAlready + 1;
    		System.out.println("-------------------> number of  qns asked already : "+numQnsAskedAlready + " limit on num qns : " +limitNumQnsToAskFinal);
    	}
	}

	private void setFinalQuestionsList() 
	{	
		if(mockInterview)
		{
			interviewMockQnsList = randomize(interviewMockQnsList);
			questionsFinalList = interviewMockQnsList; 	
			candidateAnswersList = interviewMockAnsList;
			
			System.out.println("\n\n Launching interview in Mock mode with profile "+interview_profile);
		    speak("Launching interview in Mock mode");			
		}
		else
		{
			interviewRealQnsList = randomize(interviewRealQnsList);
			questionsFinalList = interviewRealQnsList;
			
			System.out.println("\n\n Launching interview in Real mode with profile "+interview_profile);
			speak("Launching interview in Real mode");
		}
    	    	
    	qsItr  = questionsFinalList.listIterator();
	}

	private List<String> randomize(List<String> interviewQnsList) 
	{
		Set<String> randomizedQnsSet = new HashSet<String>();
		randomizedQnsList = new ArrayList<String>();
		
		if(Objects.isNull(interviewQnsList))
		{
			return interviewQnsList; //cannot randomize empty list
		}
		
		
		int numOfQns = interviewQnsList.size();
		for (int i = 0; i < numOfQns; i++) 
		{
			int random = RandomUtils.nextInt(i+1, numOfQns+1);
			randomizedQnsSet.add(interviewQnsList.get(i));  //all questions capture w/o duplicates			
			//System.out.println("-------------------> randomizer added question# : " + random + " to the final qns list");
		}
	
		randomizedQnsSet.stream()
						.forEach(qns -> randomizedQnsList.add(qns));  //add all distinct qns to the List
		
		interviewQnsList = randomizedQnsList; //recreate sequential qnsList with random qns list
		
		
		//-- integrity checks 
		/*
		int numOfQnsRandomizerSet  = randomizedQnsSet.size();
		int numOfQnsRandomizerList = randomizedQnsList.size();
		
		System.out.println("-------------------> original   List qns total  :" + numOfQns);
		System.out.println("-------------------> randomizer Set  qns total  :" + numOfQnsRandomizerSet);
		System.out.println("-------------------> randomizer List  qns total :"+ numOfQnsRandomizerList);		
		System.out.println("-------------------> randomizer complete");
		*/
		//-- integrity checks 
		
		
		return interviewQnsList;
	}

	//check intrw length limits
	private void setInterviewLength(List<String> questionsFinalList) 
	{	
		int totalNumQns = questionsFinalList.size();
		
    	if("".equals(limitNumQnsToAsk))   // user did not specify any limit -> use max questionsFinalList size 
    	{ 
    		limitNumQnsToAskFinal = totalNumQns; 
    	} 
    	else                              // user specified a limit -> use that value
    	{ 
    		try
    		{
    			limitNumQnsToAskFinal = Integer.parseInt(limitNumQnsToAsk); //expected to be an int
    		}
    		catch(Exception e)                                                 //but if numberFormatException
    		{
    			limitNumQnsToAskFinal = totalNumQns;                           //then default to max Qns size 
    		}
    	} 
    	
    	if(limitNumQnsToAskFinal < 1 || limitNumQnsToAskFinal > totalNumQns) // min-max checks 
    	{
    		limitNumQnsToAskFinal = totalNumQns;
    	}
    	
    	System.out.println("-------------------> limit Num Qns To Ask  : "+limitNumQnsToAskFinal);
	}


	private void startEvaluation() throws InterruptedException 
	{
		Thread.sleep(5000);//artificial delay
		System.out.println("-------------------> Candidate Evaluation Starts. Added 5 sec artificial delay \n\n");
		System.out.println(" We can now start the 2nd stage. Admin, when you are ready to initiate the candidate evaluation process please enter Y");		
		speak(" We can now start the 2nd stage. Admin, when you are ready to initiate the candidate evaluation process please enter Y");
				
		String replyComplete="";
		replyComplete = captureInput();
		
		while(!"Y".equalsIgnoreCase(replyComplete))
		{
			System.out.println("\n\n Admin, please enter Y to start candidate evaluation. ");
			speak("Admin, please enter Y to start candidate evaluation. ");
			replyComplete = captureInput();
		}
		
		System.out.println(" Candidate evaluation will start now");		
		speak(" Candidate evaluation will start now");
		
		prepareInterviewTranscript();
    	invokeGPTForEvaluation();
    	
    	System.out.println(" \n\n-------------------> Candidate Evaluation Finished");
	}
	
	private void prepareInterviewTranscript() throws InterruptedException 
	{
		if(mockInterview)
		{
			prepareMockInterviewTranscript();
		}
		else
		{  
			prepareRealInterviewTranscript();
		}		
	}

	private void prepareRealInterviewTranscript() 
	{
		//-- manually save the MS Teams/MS Word generated text transcript in to "C:\gen-ai-mgt-interview\interview-Transcript-1.txt
		interviewTranscript = path_base_1 + "interview-Transcript-1.txt";// save the MS Word/MS Teams real time generated transcript file here immediately after interview session finish
		
		
		System.out.println("------------------->\n\n Real Interview \n\n");
		System.out.println("Preparing interview transcript for evaluation. Please be patient  ...");
		speak("Preparing interview transcript for evaluation. Please be patient.");		
		
		System.out.println("If you use MS Teams, for the interview session, then Teams can generate this automatically for you, after the interview meeting ends ");		
		System.out.println("or, you can use the MS Word, Dictate functionality to generate this transcript, or, any 3rd party application ");		
		System.out.println("whichever approach you take, please ensure that the transcript text is saved in to the file \n\n");		
		System.out.println(interviewTranscript);
		System.out.println(" \n\n For your convinience and trial a pre-generated interview transcript is already placed. Please replace it with your specific transcript when you have it ready ");
		System.out.println("Candidate evaluation will start after you provide the interview transcript text ");
		System.out.println(" For now, if you wish, you can proceed with the pre-generated transcript for the candidate evaluation");
		System.out.println("Press any key to proceed. ");
				
		speak(" If you use MS Teams, for the interview session, then Teams can generate this automatically for you, after the interview meeting ends.");		
		speak(" or, you can use the MS Word, Dictate functionality to generate this transcript, or, any 3rd party application .");		
		speak(" whichever approach you take, please ensure that the transcript text is saved in to the file ");
		speak(interviewTranscript);
		speak(" For your convinience and trial, a pre-generated interview transcript is already placed. Please replace it, with your specific transcript when you have it ready ");
		speak(" For now, if you wish, you can proceed with the pre-generated transcript for the candidate evaluation");
		speak(" Press any key to proceed. ");
				
		String replyComplete = captureInput();
		
		System.out.println("Interview transcript confirmed"); //transcript saved at "C:\gen-ai-mgt-interview\interview-Transcript-1.txt
		speak("Interview transcript confirmed.");
		
	}

	private void prepareMockInterviewTranscript() {
		System.out.println("-------------------> Mock interview \n\n");
		
		System.out.println("\n\n Mock transcript is being prepared.");
		speak("Mock transcript is being prepared.");
		
		interviewTranscript = path_base_1 + "interview-Transcript-1.txt";
		
		System.out.println("Mock interview transcript complete"); //not real transcript. Just a file from an earlier interview
		speak("Mock interview transcript complete.");
	}
		
	private void invokeGPTForEvaluation() 
	{	
		prepareGTPRequest(); 
		callGPT();	
		processCandidateEvaluation();
	}

	private void processCandidateEvaluation() 
	{
		if (response == null || response.getChoices() == null || response.getChoices().isEmpty()) 
        {
        	System.out.println("-------------------> GPT evaluation process failed \n\n ");    	
            speak("Evaluation process failed");
        }
		else
		{
	        candidateEvaluation = response.getChoices().get(0).getMessage().getContent();
	        
	        System.out.println("------------------->\n\n Candidate evaluation complete. Here is the feedback : "+ candidateEvaluation);        
	        speak("Candidate evaluation complete. Here is the feedback ");
	        speak(candidateEvaluation);
	        
	        speak("This evaluation is now complete. Thank You and bye");
		}
	}

	private void callGPT() {
		InterviewGptRequest request = new InterviewGptRequest(model, prompt_gpt_complete);
    	System.out.println("-------------------> GPT call invoked with prompt :  \n\n "+ request.toString());    	
        speak("Evaluation is in progress. Please be patient while we await the results");
    	
        response = restTemplate.postForObject(
								                apiUrl,
								                request,
								                InterviewGptResponse.class);
	}

	private String prepareGTPRequest() 
	{
		String prompt_interview_transcript_3 = extractFileContent(interviewTranscript, null); //transcript should be available at this location immediately after the intrw session		
		prompt_gpt_complete = prompt_gpt_complete + prompt_interview_transcript_3;
		
		return prompt_gpt_complete;
	}
	
	
	private void askQuestion(String question) throws InterruptedException  
	{
		System.out.println("\n\n "+question);
		Thread.sleep(3000);
		speak(question);
	}
	
	private void getAnswer(boolean mockInterview) throws InterruptedException //decide if real or mock interview flow
	{
		if(mockInterview) 
		{
			if(ansItr!=null && ansItr.hasNext())//so that even if mock ans sheet is not provided
			{
				getAnswer(ansItr.next());
			}
		}
		else
		{
			getAnswer();
		}
	}
	
	private void getAnswer(String answer) throws InterruptedException  //in the mock interview pre-arranged answers will be spoken 
	{
		Thread.sleep(3000);
		answer = answer.replaceFirst("Answer :", "");//adjustment
		speak(answer);
		Thread.sleep(3000);
	}
	
	private void getAnswer() //in this real interview - candidate's answers will be live captured and auto text transcripted w/ Winword/MS Teams dictate 
	{	
		String replyComplete="";
		replyComplete = captureInput();
		
		while(!"Y".equalsIgnoreCase(replyComplete))
		{
			System.out.println("\n\n Is your answer complete? If so, enter Y ");
			speak("Is your answer complete? If so, enter Y ");
			replyComplete = captureInput();
		}	    
	    
	    System.out.println("\n\n Candidate ready for the next question ? " + replyComplete);
	}
	
	private String captureInput() //in this real interview - candidate's answers will be live captured and auto text transcripted w/ Winword/MS Teams dictate 
	{	
		Scanner sc = new Scanner(System.in); //blocked until user enters an input 
	    String replyComplete = sc.nextLine();
	    return replyComplete;
	}

	private void concludeInterview() 
	{
		//only after the MS Teams ends then the text transcript wil be generated
		System.out.println("\n\n Your interview is now complete. Candidate, you may now signout from this meeting. Thank you for your time and patience");
		speak("Your interview is now complete. Candidate, you may now signout from this meeting. Thank you for your time and patience.");	
	}

	private void speak(String message)  
	{
		textToSpeech.dospeak(message, "kevin16");
    	//speak("يخزن هذا الموقع ملفات تعريف الارتباط على جهاز الكمبيوتر الخاص بك. تُستخدم ملفات تعريف الارتباط هذه لجمع معلومات حول كيفية تفاعلك مع موقعنا الإلكتروني والسماح لنا بتذكرك. نحن نستخدم هذه المعلومات لتحسين وتخصيص تجربة التصفح الخاصة بك وللتحليلات والقياسات حول زوارنا على هذا الموقع والوسائط الأخرى. لمعرفة المزيد حول");
	}
	
	//extracted line will be added to StringBuilder content , List contentList whichever is non null 
	private String extractFileContent(String fileName, List contentList )
	  {	
		fileName = preProcessFile(fileName);
		
		StringBuilder content = new StringBuilder();
		String line;
		File file = new File(fileName);
		FileReader fr;

		try {
	        
			fr = new FileReader(file);	
			BufferedReader br = new BufferedReader(fr);
	        //System.out.println("Reading text file using FileReader");
	        
	        while ((line = br.readLine()) != null) 
	        {
	            
	        	if(content !=null )
	        	{
		            content.append(line); //add the extracted line
		            content.append("\n\n");
	        	}
	        	
	        	if(contentList !=null ) 
	        	{
	        		contentList.add(line); //add the extracted line
	        	}
	        	
	        }
	        br.close();
	        fr.close();
	        
		} catch (Exception e) 
		{
			e.printStackTrace();
		}
	        return content.toString();
	    }

	private String preProcessFile(String fileName) 
	{   // construct full file name with profile included
		
		if(interview_profile==null)
		{
			return fileName;
		}
		
		fileName = fileName.replaceFirst("PLACE_HOLDER_PROFILE_PATH", interview_profile); 
		return fileName; // C:\gen-ai-mgt-interview\interview_profile\software_engineer\interview-context-criteria-1.txt
	}	
	
}
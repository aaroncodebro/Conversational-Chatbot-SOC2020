import pyaudio
import speech_recognition as sr

r = sr.Recognizer()

with sr.Microphone() as source:
	r.adjust_for_ambient_noise(source) 
    # read the audio data from the default microphone
	print("Speek something......")
	audio = r.listen(source)
    
try:
	text = r.recognize_google(audio) 
	print (text)        
      
        
    #error occurs when google could not understand what was said 
      
except sr.UnknownValueError: 
        print("Google Speech Recognition could not understand audio") 
      
except sr.RequestError as e: 
        print("Could not request results from Google Speech Recognition service; {0}".format(e)) 



input()
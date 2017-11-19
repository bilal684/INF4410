from threading import Thread
from urllib.request import urlopen
from time import sleep
import sys, time

def main(ip):
	start = time.time()
	threads = []
	for i in range(0,40):
		t = Thread(target=callRequestToServer, args=["http://"+ ip + ":8000"])
		t.start()
		threads.append(t)
		#Ici le sleep est necessaire, sinon le serveur ferme la connection (trop de connection arrive en meme temps)
		sleep(0.5)
	for t in threads:
		t.join()
	end = time.time()
	print("Total execution time : " + str(end - start) + " seconds")
	print("Total execution time per request : " + str((end - start)/40.0) + " seconds")

def callRequestToServer(url):
	return urlopen(url)
	
main(str(sys.argv[1]))
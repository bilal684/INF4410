from threading import Thread
from urllib.request import urlopen
from time import sleep
import sys, time

def main(ip, port):
	threads = []
	start = time.time()
	for i in range(0,40):
		t = Thread(target=callRequestToServer, args=["http://"+ ip + ":" + port])
		t.start()
		threads.append(t)
		sleep(0.5)
	for t in threads:
		t.join()
	end = time.time()
	print("Execution time : " + str(end - start) + " seconds")
def callRequestToServer(url):
	return urlopen(url)
	
main(str(sys.argv[1]), str(sys.argv[2]))
from threading import Thread
from urllib.request import urlopen
from time import sleep
import sys
def main(ip, port):
	threads = []
	for i in range(0,20):
		t = Thread(target=callRequestToServer, args=["http://"+ ip + ":" + port])
		t.start()
		threads.append(t)
		sleep(0.05)
	for t in threads:
		t.join()

def callRequestToServer(url):
	return urlopen(url)
	
main(str(sys.argv[1]), str(sys.argv[2]))
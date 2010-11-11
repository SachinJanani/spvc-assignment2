package dk.itu.spvc.bliploc;

public class Threads extends Thread {
	
	Thread t = new Thread();
	
	int count;
	  Threads() {
	    count = 0;
	  }
	  
	  public void run() {
	    System.out.println("MyThread starting.");
	    try {
	      do {
	        Thread.sleep(30000);
	        System.out.println("In MyThread, count is " + count);
	        count++;
	      } while (count < 5);
	    } catch (InterruptedException exc) {
	      System.out.println("MyThread interrupted.");
	    }
	    System.out.println("MyThread terminating.");
	  
	
	
	

	    
	  	    do {
	      System.out.println("In main thread.");
	      try {
	        Thread.sleep(250);
	      } catch (InterruptedException exc) {
	        System.out.println("Main thread interrupted.");
	      }
	    } while (t.count != 5);
	    System.out.println("Main thread ending.");
	  }
 }


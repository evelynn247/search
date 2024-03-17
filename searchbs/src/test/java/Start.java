

import org.springframework.context.support.ClassPathXmlApplicationContext;


public class Start {
	public static void main(String[] args) throws Exception {
		ClassPathXmlApplicationContext context = 
        		new ClassPathXmlApplicationContext(new String[] {"spring/search-as.xml"});
		context.start();
        System.out.println("start");
        System.in.read(); 
        
    }
}



import com.biyao.search.as.server.SearchDubboServiceImpl;
import com.biyao.search.as.service.enums.PlatformEnum;
import com.biyao.search.as.service.model.request.ASSearchRequest;
import com.biyao.search.as.service.model.response.ASSearchResponse;
import com.biyao.search.common.model.ASProduct;
import com.biyao.search.common.model.RPCResult;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class StartTest {
	public static void main(String[] args) throws Exception {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(new String[] {"spring/search-as.xml"});
		context.start();
        System.out.println("start");
        //test(context);
        System.in.read(); 
        
    }
	//PC接口测试
	private static void test(ClassPathXmlApplicationContext context){
		SearchDubboServiceImpl searchDubboService = (SearchDubboServiceImpl) context.getBean("asSearchService");
		ASSearchRequest asSearchRequest = new ASSearchRequest();
		asSearchRequest.setQuery("测试");
		asSearchRequest.setUuid("9190912161621576da9f851a5acee0000000");
		asSearchRequest.setSid("4324153265426");
		asSearchRequest.setPageSize(100);
		asSearchRequest.setPlatform(PlatformEnum.ANDROID);
		RPCResult<ASSearchResponse<ASProduct>> pcResult = searchDubboService.search(asSearchRequest);
		System.out.println("test");
	}
}

package com.biyao.search.bs.server.common.config;

import java.net.InetAddress;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * @description: es连接客户端初始化
 * @author: luozhuo
 * @date: 2017年2月23日 下午4:40:22
 * @version: V1.0.0
 */
@Service
public class ESClientConfig {
	private static final Log logger = LogFactory.getLog(ESClientConfig.class);

	private static TransportClient client;

	@Autowired
	private void init(@Value("${es.cluster.master.ip}") String ip,
			@Value("${es.cluster.master.port}") int port) {
		Settings settings = Settings.builder()
				.put("cluster.name", "biyaomall-search")
				.put("client.transport.sniff", true).build();

		try {
			client = new PreBuiltTransportClient(settings)
					.addTransportAddress(new InetSocketTransportAddress(
							InetAddress.getByName(ip), port));
		} catch (Exception e) {
			logger.error("初始化es客户端出现异常，ip：" + ip + ", port：" + port, e);
		}
	}

	public static  TransportClient getESClient() {
		return client;
	}
}

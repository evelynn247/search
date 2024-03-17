package com.biyao.search.as.server.cache;

import com.biyao.client.model.Product;
import com.biyao.search.as.server.bean.ProductInfoFromPdc;
import com.biyao.search.as.server.rpc.ProductRpcService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.*;

/**
 * @author xiafang@idstaff.com
 * @date 2020/04/27
 **/
@Component
@Slf4j
public class ProductCache {

    @Resource
    private ProductRpcService productRpcService;

    private static final int PAGE_SIZE = 100;

    /**
     * 常量CUSTOMIZE = 1表示定制商品
     */
    private static final Byte CUSTOMIZE = 1;

    private Map<Long, ProductInfoFromPdc> searchProductInfoMap;

    @PostConstruct
    public void init() {
        log.info("[操作日志]初始化商品缓存...");
        refresh();
        log.info("[操作日志]初始化商品完成！");
    }

    public void refresh() {
        try {
            List<ProductInfoFromPdc> searchProductInfoList = getProductList();
            Map<Long, ProductInfoFromPdc> tempMap = new HashMap<>();
            if (searchProductInfoList != null && searchProductInfoList.size() > 0) {
                searchProductInfoList.stream().forEach(item -> {
                    tempMap.put(item.getProductId(), item);
                });
            }
            //同步商品数据时，如果本次同步商品数据为空，则不更新内存中数据
            if (CollectionUtils.isEmpty(tempMap)) {
                log.error("[严重异常][同步商品数据异常]获取商品数据为空，可能系统出现问题了，需要立即处理");
            } else {
                this.searchProductInfoMap = tempMap;
            }
            log.info("[操作日志]商品缓存刷新成功");
        } catch (Exception e) {
            log.error("[严重异常][dubbo异常]更新商品缓存失败:", e);
        }
    }


    /**
     * 从pdc拉取全量商品
     *
     * @return
     */
    private List<ProductInfoFromPdc> getProductList() {
        List<ProductInfoFromPdc> res = new LinkedList<>();
        List<Product> temp = new LinkedList<>();
        int pageIndex = 1;
        while (true) {


            try {
                List<Product> productLst = productRpcService.queryProductByPage(pageIndex);
                if(CollectionUtils.isEmpty(productLst)) {
                    break;
                }
                pageIndex++;
                temp.addAll(productLst);
            } catch (Exception e) {
                log.error("[严重异常]同步商品数据rpc接口异常，本次同步失败，原有内存缓存数据将被清空", e);
                return null;
            }
        }

        if (temp.size() > 0) {

            for (Product pdcProduct : temp) {
                if (CUSTOMIZE.equals(pdcProduct.getSupportTexture()) || pdcProduct.getShelfStatus() == 0) {
                    continue;
                }
                ProductInfoFromPdc searchProductInfo = ProductInfoFromPdc.builder()
                        .productId(pdcProduct.getProductId())
                        .suId(pdcProduct.getSuId())
                        .price(pdcProduct.getPrice())
                        .thirdCategoryId(pdcProduct.getThirdCategoryId())
                        .shelfStatus(pdcProduct.getShelfStatus())
                        .refundRate(pdcProduct.getRefundRate())
                        .frontThirdCategoryIds(pdcProduct.getfCategory3Ids())
                        .frontThirdCategoryNames(pdcProduct.getfCategory3Names())
                        .build();
                res.add(searchProductInfo);
            }
        }

        return res;
    }

    /**
     * 根据商品id查询商品退货退款率
     *
     * @param productId
     * @return
     */
    public double getRefundRateByPid(Long productId) {
        if (this.searchProductInfoMap == null) {
            return 0;
        }
        ProductInfoFromPdc productInfoFromPdc = searchProductInfoMap.get(productId);
        if (productInfoFromPdc == null) {
            return 0;
        }
        return productInfoFromPdc.getRefundRate();
    }

    /**
     * 根据商品id查询商品三级类目id
     *
     * @param productId
     * @return
     */
    public Long getThirdCategoryId(Long productId) {
        if (this.searchProductInfoMap == null) {
            return null;
        }
        ProductInfoFromPdc productInfoFromPdc = searchProductInfoMap.get(productId);
        if (productInfoFromPdc == null) {
            return null;
        }
        return productInfoFromPdc.getThirdCategoryId();
    }
    /**
     * 获取内存缓存中商品信息
     * @param productId
     * @return
     */
    public ProductInfoFromPdc getProductInfo(long productId) {
    	 if (this.searchProductInfoMap == null) {
             return null;
         }
    	 return searchProductInfoMap.get(productId);
    }
}

package com.biyao.search.bs.server.common.consts;

public class ElasticSearchConsts {
	/**
	 * es索引名称
	 */
	@Deprecated
	public static final String ES_PRODUCT_INDEX_NAME = "biyaomall";
	/**
	 * 搜索商品索引别名
	 */
	public final static String BY_MALL_ALIAS = "by_mall";
	/**
	 * 搜索衍生商品索引别名
	 */
	public final static String BY_MALL_DERIVE_ALIAS = "by_mall_derive";
	/**
	 * es商品类型名称
	 */
	public static final String ES_PRODUCT_TYPE_NAME = "product";
	/**
	 * 搜索时分词器
	 */
	public static final String SEARCH_ANALYZER_TYPE = "ik_smart";
	/**
	 * 召回结果字段
	 */

	public static final String[] FETCH_SOURCE = {"title", "shortTitle", "suId", "image", "price", "saleMode", "groupPrice", 
		"salePoint", "supplierBackground", "commentNum", "goodCommentNum", "labels", "productId", "weekSaleNum", "activities", "attribute","queryExtension"};
	/**
	 * 索引中商品属性字段
	 */
	public static final String ATTRIBUTE_FIELD = "attribute";
	/**
	 * 索引中商品价格字段
	 */
	public static final String PRICE_FIELD = "price";
	public static final String PRICE_FACET_KEY = "价格区间";
	public static final String ACTIVITY_FACET_KEY = "活动";

	/**
	 * 召回匹配字段
	 */
	public static final String[] MATCH_FIELDS = {"title", "alias", "shortTitle",
			"salePoint", "supplierName",
			"fcategory1Names", "fcategory2Names", "fcategory3Names",
			"category1Name", "category2Name", "category3Name", "queryExtension"};
	/**
	 * 分销场景活动标签召回匹配字段
	 */
	public static final String[] ACTIVITY_TAG_MATCH_FIELDS={"title", "alias", "shortTitle", "salePoint", "supplierName",
			"fcategory1Names", "fcategory2Names", "fcategory3Names",
			"category1Name", "category2Name", "category3Name", "queryExtension","activityTag"};
	/**
	 * 相关词匹配字段
	 */
	public static final String[] RELETED_MATCH_FIELDS = {"title", "alias", "shortTitle",
			"supplierName",
			"fcategory1Names", "fcategory2Names", "fcategory3Names",
			"category1Name", "category2Name", "category3Name", "queryExtension","productWord"};
	/**
	 * 产品词召回匹配字段list
	 */
	public static final String[] PRODUCT_MATCH_FIELDS = {"title", "alias", "shortTitle",
			"salePoint", "supplierName",
			"fcategory1Names", "fcategory2Names", "fcategory3Names",
			"category1Name", "category2Name", "category3Name", "queryExtension","productWord"};

	/**
	 * 产品词召回匹配字段
	 */
	public static final String[] PRODUCT_WOED = {"productWord"};
	/**
	 * 实验召回匹配字段
	 */
	public static final String[] MATCH_FIELDS_EXP = {"title", "alias", "shortTitle",
			"salePoint", "supplierName",
			"fcategory1Names", "fcategory2Names", "fcategory3Names",
			"category1Name", "category2Name", "category3Name","queryExtension", "testExtension"};

	/**
	 * 衍生商品召回匹配字段
	 */
	public static final String[] MATCH_FIELDS_DERIVE = {"title", "alias", "shortTitle",
			"salePoint", "supplierName",
			"fcategory1Names", "fcategory2Names", "fcategory3Names",
			"category1Name", "category2Name", "category3Name"};

/*	*//**
	 * 后台类目字段（类目预测项目用于过滤）
	 *//*
	public static final String[] CATEGORY_FIELDS = {
			"category1NamePrediction", "category2Name", "category3Name"};*/
	public static final String CATEGORY1NAME_PREDICTION_FIELDS = "category1NamePrediction";
	public static final String CATEGORY2NAME_PREDICTION_FIELDS = "category2NamePrediction";
	public static final String CATEGORY3NAME_PREDICTION_FIELDS = "category3NamePrediction";

	/**
	 * tag召回匹配字段
	 */
	public static final String[] TAG_MATCH_FIELDS = {"alias"};

	/**
	 * 性别字段
	 */
	public static final String SEX_LABEL_FIELD = "sexLabel";

	/**
	 * 季节字段
	 */
	public static final String SEASON_LABEL_FIELD = "seasonLabel";
	/**
	 * suId字段
	 */
	public static final String PID_FIELD = "productId";
	/**
	 *  商品模型类型字段
	 */
	public static final String MODEL_TYPE = "modelType";

	/**
	 * 店铺名称字段，用于店铺内搜索
	 */
	public static final String SUPPLIER_ID = "supplierId";

	/**
	 * 江湖计划cps分销商品池新增字段，限制商品池时复用该字段
	 */
	public static final String PRODUCT_POOL = "productPool";

	/**
	 * 必要造物标识，isCreator 1:是必要造物商品  0：不是必要造物商品
	 */
	public static final String IS_CREATOR_CON = "isCreator";

	/**
	 * 必要造物标识-YES
	 */
	public static final int IS_CREATOR_FLAG_1 = 1;


	/**
	 * es   商品可支持渠道
	 */
	public static final String SUPPORT_CHANNEL_CONS = "supportChannel";


	/**
	 * es   商品可支持渠道  1：必要主站
	 */
	public static final int SUPPORT_CHANNEL_CONS_BYZZ = 1;


	/**
	 * es   商品可支持渠道  2: 必要分销
	 */
	public static final int SUPPORT_CHANNEL_CONS_BYFX = 2;

}

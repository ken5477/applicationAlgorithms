import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 抽奖概率计算类
 *
 * <p>参考dubbo负载均衡RandomLoadBalance算法
 * RandomLoadBalance 是dubbo负载均衡加权随机算法的具体实现
 *
 * @author ken5477
 * @since   1.0
 */
public class DoLotteryUtil {

    /**
     * Creates a new random number generator. This constructor sets
     * the seed of the random number generator to a value very likely
     * to be distinct from any other invocation of this constructor.
     */
    private static final Random random = new Random();


    /**
     * 抽奖方法
     *
     * @param resourceList  参与抽奖资源
     * @param countFlag     是否置数量为0的资源不参与抽奖
     * @return Resource 资源
     */
    protected static Resource doSelect(List<Resource> resourceList, Boolean countFlag) {
        if (null == resourceList || resourceList.size() == 0) {
            return null;
        }
        //去除资源列表中  资源数量为0的资源
        // countFlag为true 资源数量为0的资源不参与抽奖
        if(countFlag) {
            Iterator it = resourceList.iterator();
            while (it.hasNext()) {
                Resource resourceIt = (Resource) it.next();
                if (resourceIt.getTotalCount() <= 0L) {
                    it.remove();
                }
            }
        }
        int size = resourceList.size();
        //奖品已经抽完
        if(0 == size){
            return null;
        }
        //如果当前资源列表中只有一个资源，直接返回
        if (1 == size) {
            return resourceList.get(0).getTotalCount() > 0L ? resourceList.get(0) : null;
        }

        int totalPercent = 0;
        boolean samePercent = true;
        // 下面这个循环有两个作用，第一是计算中奖概率 totalPercent，
        // 第二是检测每个资源的中奖概率是否相
        for (int i = 0; i < size; i++) {
            int percent = resourceList.get(i).getProbabilityValue();
            //累加概率
            totalPercent += percent;

            // 检测当前资源的概率是否与上一个资源的概率相等
            // 不同的话，则将samePercent置为false
            if (samePercent && i > 0 && percent != resourceList.get(i - 1).getProbabilityValue()) {
                samePercent = false;
            }
        }

        //获取随机数，并计算随机数落在哪个区间
        if (totalPercent > 0 && !samePercent) {
            //获取一个「0-totalPercent」之间的随机数，并计算随机数落在哪个区间
            int luckyNo = random.nextInt(totalPercent);
            System.out.println("幸运数字："+luckyNo);
            // 循环让 luckyNo 数减去当前资源的概率值，当 luckyNo 小于0时，返回相应的 资源。
            // 举例说明一下，我们有 resource = [A, B, C]，percent = [50, 30, 20]，luckyNo = 70。
            // 第一次循环，luckyNo - 50 = 20 > 0，即 luckyNo > 50，
            // 表明其不会落在资源A 对应的区间上。
            // 第二次循环，luckyNo - 30 = -10 < 0，即 50 < luckyNo < 80，
            // 表明其会落在资源 B 对应的区间
            for (int i = 0; i < size; i++) {
                luckyNo -= resourceList.get(i).getProbabilityValue();
                if (luckyNo < 0) {
                    return resourceList.get(i);
                }
            }

        }

        //如果所有资源概率相同，直接随机返回一个即可
        return resourceList.get(random.nextInt(size));
    }


    /**
     *  抽奖测试main方法
     *  抽奖结果统计  不考虑奖品数量  100000000次 概率分别为
     *  资源3概率 30   资源2概率 20   资源1概率 50 的结果：
     *  {资源3=29999609, 资源2=19997269, 资源1=50003122}
     *  符合概率分布
     * @param args
     */
    public static void main(String[] args) {
        List<Resource> resourceList = new ArrayList<>();
        Resource resource = new Resource();
        resource.setId("1");
        resource.setResourceName("资源1");
        resource.setProbabilityValue(80);
        resource.setTotalCount(50L);
        resourceList.add(resource);

        Resource resource2 = new Resource();
        resource2.setId("2");
        resource2.setResourceName("资源2");
        resource2.setProbabilityValue(10);
        resource2.setTotalCount(20L);
        resourceList.add(resource2);


        Resource resource3 = new Resource();
        resource3.setId("3");
        resource3.setResourceName("资源3");
        resource3.setProbabilityValue(10);
        resource3.setTotalCount(30L);
        resourceList.add(resource3);

        Map<String, AtomicInteger> resultMap = new HashMap<>();


        for (int i = 0; i < 100; i++) {
            Resource returnResource = doSelect(resourceList, true);
            if (null == returnResource) {
                System.out.println("奖品已抽完!");
            } else {
                System.out.println("获奖的是:" + returnResource.getResourceName());
                if (resultMap.containsKey(returnResource.getResourceName())) {
                    AtomicInteger count = resultMap.get(returnResource.getResourceName());
                    resultMap.put(returnResource.getResourceName(), new AtomicInteger(count.incrementAndGet()));
                } else {
                    resultMap.put(returnResource.getResourceName(), new AtomicInteger(1));
                }

                for (int j = 0; j < resourceList.size(); j++) {
                    if (returnResource.equals(resourceList.get(j))) {
                        resourceList.get(j).decrement();
                    }
                }
            }
        }
        System.out.println(resultMap);

    }


    public static class Resource {

        /**
         * 资源id
         */
        private String id;

        /**
         * 资源名称
         */
        private String resourceName;

        /**
         * 资源总数
         */
        private Long totalCount;

        /**
         * 概率值
         */
        private int probabilityValue;

        public String getId() {
            return id;
        }

        public Resource setId(String id) {
            this.id = id;
            return this;
        }

        public String getResourceName() {
            return resourceName;
        }

        public Resource setResourceName(String resourceName) {
            this.resourceName = resourceName;
            return this;
        }

        public Long getTotalCount() {
            return totalCount;
        }

        public Resource setTotalCount(Long totalCount) {
            this.totalCount = totalCount;
            return this;
        }

        public int getProbabilityValue() {
            return probabilityValue;
        }

        public Resource setProbabilityValue(int probabilityValue) {
            this.probabilityValue = probabilityValue;
            return this;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Resource)) {
                return false;
            }
            Resource resource = (Resource) o;
            return getProbabilityValue() == resource.getProbabilityValue() &&
                    Objects.equals(getId(), resource.getId()) &&
                    Objects.equals(getResourceName(), resource.getResourceName()) &&
                    Objects.equals(getTotalCount(), resource.getTotalCount());
        }

        @Override
        public int hashCode() {
            return Objects.hash(getId(), getResourceName(), getTotalCount(), getProbabilityValue());
        }

        /**
         * 资源数量扣减
         */
        synchronized public void decrement() {
            this.totalCount--;
        }
    }

}

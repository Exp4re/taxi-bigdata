import pandas as pd
import matplotlib.pyplot as plt
import glob

# 固定字体为英文
plt.rcParams['font.sans-serif'] = ['DejaVu Sans']
plt.rcParams['axes.unicode_minus'] = False

# 文件路径
p1 = glob.glob("/home/chenxingchi/taxi_result_local/result/vendor_stat/*.csv")[0]
p2 = glob.glob("/home/chenxingchi/taxi_result_local/result/passenger_stat/*.csv")[0]
p3 = glob.glob("/home/chenxingchi/taxi_result_local/result/fare_stat/*.csv")[0]
p4 = glob.glob("/home/chenxingchi/taxi_result_local/result/day_stat/*.csv")[0]

# 读取数据
df_vendor = pd.read_csv(p1, header=None, names=["VendorID", "OrderCount", "TotalRevenue"])
df_pass = pd.read_csv(p2, header=None, names=["PassengerNum", "OrderCount"])
df_fare = pd.read_csv(p3, header=None, names=["FareRange", "OrderCount"])
df_day = pd.read_csv(p4, header=None, names=["Date", "OrderCount"])

# 把车费区间中文替换成英文
map_dict = {
    "0~5美元":"0-5 USD",
    "5~10美元":"5-10 USD",
    "10~20美元":"10-20 USD",
    "20美元以上":"Over 20 USD"
}
df_fare["FareRange"] = df_fare["FareRange"].replace(map_dict)

# 绘制四张子图
fig, ax = plt.subplots(2, 2, figsize=(16,10))
# 左上：服务商订单量
ax[0,0].bar(df_vendor["VendorID"], df_vendor["OrderCount"], color="#2E86AB")
ax[0,0].set_title("Order Count of Each Vendor")
# 右上：服务商营收
ax[0,1].bar(df_vendor["VendorID"], df_vendor["TotalRevenue"], color="#A23B72")
ax[0,1].set_title("Total Revenue of Each Vendor(USD)")
# 左下：乘车人数分布
ax[1,0].bar(df_pass["PassengerNum"], df_pass["OrderCount"], color="#F18F01")
ax[1,0].set_title("Order Distribution by Passenger Number")
# 右下：车费占比（标签全英文）
ax[1,1].pie(df_fare["OrderCount"], labels=df_fare["FareRange"], autopct="%.2f%%")
ax[1,1].set_title("Order Ratio by Fare Range")

plt.tight_layout()
plt.savefig("/home/chenxingchi/taxi_final.png", dpi=300)
print("✅ 成功生成：taxi_final.png")

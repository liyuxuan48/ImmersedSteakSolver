# Heat Lab for iPhone

中英双语本地计算 PWA。Safari 打开 https://liyuxuan48.github.io/heat-lab/ ，分享 → 添加到主屏幕。不是签名 IPA；离线可用性依赖浏览器缓存。刷新会丢失模拟结果。

## 当前数值方法

网页版现在逐状态求解三维浸没层约束：未知表面温度跳跃与内侧 Robin 热流耦合，修正后的法向梯度满足约束。辅助外侧使用齐次 Robin 条件控制离散振荡，热量单独计入辅助域交换。边界 CSV 仅导出物理内侧热流。与旧 Android APK 的掩膜归一化闭合不同，不声明 Julia 数值一致性。

完整推导和结果见 [CONSTRAINED_METHOD.md](../docs/CONSTRAINED_METHOD.md)。`bash scripts/test-ios.sh` 覆盖算子伴随、边界约束、球体加密、翻面、平衡、耗散、能量、导出、熟度峰值与翻译。小边界残差不代表温度已收敛，粗网格强换热误差仍较大。

## 界面与本地运行

顶部可切换 English / 中文。三维显示可选表面温度或边界条件；切片可选当前温度或历史最高温度熟度。15° 朝下法向范围使用锅面条件，其余使用空气对流。锅面系数为 0 时全表面对流，不使用接触带深度。网格、材料和已保存设置继续可用。

```sh
python3 -m http.server 8765 --directory dist
```

手机离线安装需要 HTTPS。当前浏览器测试不能替代 iPhone 真机性能、安装或分享验证。

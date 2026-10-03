/* Presentation-only localization. Preserve source text so changing language never
 * rebuilds the worker, changes input values, or modifies numerical/export data. */
(function(root){
'use strict';
const en={
"边界约束残差":"Boundary constraint residual","表面求解迭代次数":"Surface solver iterations","辅助外域界面交换":"Auxiliary exterior interface exchange","方程、离散与验证":"Equations, discretization & validation","辅助外域界面与外边界的热量交换均属于数值计算域，不是实际散热。小约束残差不代表温度已收敛；请比较不同网格分辨率。":"Auxiliary interface and outer-box heat exchanges are numerical-domain effects, not physical cooling. A small constraint residual does not establish temperature convergence; compare grid resolutions.","采用 JuliaIBPM 浸没层框架的约束方程，在每个时间步求解未知表面温度跳跃，使修正法向梯度与热流条件一致。物理内侧使用锅面或空气 Robin 条件；辅助外侧使用齐次 Robin 条件维持初始参考温度。表面温度由两侧迹重构，不再使用旧的掩膜归一化表面闭合。":"Uses the JuliaIBPM immersed-layer constraint formulation: each time step solves for the unknown surface temperature jump so the corrected normal gradient satisfies the heat-flux conditions. The physical interior uses pan or air Robin conditions; the auxiliary exterior uses homogeneous Robin conditions at the initial reference temperature. Two-sided traces replace the old mask-normalized surface closure.","显式时间步在翻面时刻截断，每个状态均求解表面约束。球体解析解的误差随网格加密减小，但粗网格高换热率误差仍较大。辅助外侧条件用于控制离散振荡；这是独立三维 Robin 扩展，不是直接运行 Julia 包，也未建立与 Julia 的数值一致性。":"Explicit time steps end exactly at flips, with a surface constraint solve at every state. Sphere benchmark errors decrease with refinement, but coarse-grid errors remain substantial for strong heat transfer. The auxiliary exterior condition controls discrete oscillations. This is an independent 3D Robin extension, not execution of Julia packages or demonstrated numerical parity with Julia.",
"未达一分熟":"Below rare","一分熟":"Rare","三分熟":"Medium rare","五分熟":"Medium","七分熟":"Medium well","全熟":"Well done","切片显示":"Slice display","当前温度":"Current temperature","熟度（历史最高温度）":"Cookedness (peak temperature)","温度参考：Certified Angus Beef":"Temperature reference: Certified Angus Beef","每个单元使用本次模拟每个时间步的历史最高温度，冷却和翻面不会降低熟度；重置会清除历史。以公开目标温度作为分类下限，并非熟度反应动力学或食品安全判断。":"Each cell uses its highest temperature across every time step of this run. Cooling and flipping do not lower its category; resetting clears history. Published target temperatures are used as category lower bounds, not as a doneness-kinetics or food-safety model.",
'未解析出符合角度条件的接触面，请提高分辨率或关闭锅面换热。':'No contact facets within the normal-angle threshold resolved. Increase resolution or disable pan heating.',
"三维显示":"3D view","表面温度":"Surface temperature","边界条件":"Boundary conditions","2–4；平面上下表面":"2–4; planar top and bottom","颜色显示重构表面温度，色标随当前表面范围变化；内部温度见下方切片。":"Colors show reconstructed surface temperature with a scale that follows the current surface range. See the slice below for interior temperatures.","锅面接触由朝外单位法向与竖直向下方向的夹角决定：夹角不超过 15° 时使用锅面 Robin 边界条件（w = 1），其余表面使用空气对流（w = 0）。此规则也包含符合角度条件的圆润边缘，不要求网格面完全平坦。锅面换热系数设为 0 时，整个表面使用空气对流。翻面后按旋转后的法向重新选择接触面。不使用接触带深度。":"Pan contact uses the angle between the outward unit normal and vertically downward: at most 15° uses the pan Robin boundary condition (w = 1); all other surfaces use air convection (w = 0). This includes rounded rim facets within the angle threshold; facets need not be completely flat. Setting the pan coefficient to 0 applies convection everywhere. Flips select contact using the rotated normals. No contact-depth band is used.","使用四点正则化 delta 函数、交错网格双层算子和校准的表面温度重构。使用双精度数值；网页版采用平面接触几何，与原 Android 接触带模型不同。它是独立 immersed-layer 变体，不是 JuliaIBPM 原始 Neumann 约束算法。":"Uses a four-point regularized delta, staggered double-layer operator and calibrated surface-temperature reconstruction in double precision. The web model uses planar contact geometry, unlike the original Android contact-band model. This is an independent immersed-layer variant, not JuliaIBPM’s reference Neumann-constraint algorithm.","显式时间步在翻面时刻精确截断。球体对流解析解、法向角度和翻面测试用于核验实现；粗网格的加密误差不一定单调。辅助域交换的数值热量会单独报告，不能将很小的能量记账残差理解为准确的物理预测。":"Time steps end exactly at flips. Analytical Robin-sphere, normal-angle and flip tests check the implementation. Coarse-grid refinement errors need not be monotonic. Auxiliary-box heat exchange is numerical; a small energy-accounting residual does not guarantee physical accuracy.",
'Heat Lab · 3D 牛排':'Heat Lab · 3D Steak',
'3D 牛排瞬态传热模拟：锅面接触、空气对流与定时翻面。':'3D transient steak heat simulation with pan contact, air convection and scheduled flips.',
'安装':'Install','功能':'Navigation','模拟':'Simulation','设置':'Setup','方法':'Method',
'准备就绪':'Ready','模拟进度':'Simulation progress','开始模拟':'Run simulation','重置':'Reset',
'可拖动旋转的三维牛排网格':'Drag to rotate the 3D steak mesh','拖动旋转':'Drag to rotate',
'暴露表面':'Exposed surface','锅面接触':'Pan contact','＋ 冷点':'+ Cold point','最低内部温度':'Coldest resolved core',
'下次翻面':'Next flip','初始内部温度均匀':'Uniform initial interior temperature','锅面传热':'Pan heat transfer','空气换热':'Air heat transfer',
'内部温度切片':'Interior temperature slice','定位冷点':'Find cold point','切片方向':'Slice plane',
'XY · 沿厚度 z':'XY · through thickness z','XZ · 沿宽度 y':'XZ · along width y','YZ · 沿长度 x':'YZ · along length x',
'三维温度场切片':'Slice of the 3D temperature field',
'切片显示所有内部网格单元的温度，包括靠近边界的单元。边界附近温度受浸没边界重构误差影响较大；冷点和统计仍仅取可解析内部。切片坐标随牛排移动。':'Slices show temperatures in all interior cells, including cells near the boundary. Near-boundary temperatures are more sensitive to immersed-boundary reconstruction error; cold points and statistics still use only the resolved core. Slice coordinates stay attached to the steak.',
'数值诊断':'Numerical diagnostics',
'辅助计算域的热量交换是数值误差，不是实际散热。请比较不同网格分辨率。':'Heat exchange with the auxiliary box is numerical error, not physical cooling. Compare different grid resolutions.',
'导出结果':'Export results','内部 CSV':'Core CSV','温度历史':'Temperature history','边界 CSV':'Boundary CSV','网格 STL':'Mesh STL',
'导出当前显示的时刻。iPhone 可选择“存储到文件”。':'Export the displayed snapshot. On iPhone, choose Save to Files.',
'设置模拟':'Simulation setup','应用后将清除上一次结果。':'Applying settings clears the previous results.',
'圆润牛排':'Rounded steak','仅烤箱':'Oven only','应用并重新开始':'Apply and reset',
'锅面、空气与翻面':'Pan, air & flipping','初始温度':'Initial temperature','锅面温度':'Pan temperature','空气温度':'Air temperature',
'锅面换热系数':'Pan transfer coefficient','空气换热系数':'Air transfer coefficient','接触带深度':'Contact band depth',
'mm；0 关闭锅面接触':'mm; 0 disables pan contact','翻面时刻':'Flip times','秒，逗号分隔；留空不翻面':'Seconds, comma-separated; blank for no flips','例如 120, 240':'e.g. 120, 240',
'牛排形状':'Steak shape','名义长度':'Nominal length','名义宽度':'Nominal width','厚度':'Thickness','圆润指数':'Roundness exponent',
'2 椭球 · 4 较平坦':'2 ellipsoid · 4 flatter faces','轮廓变化':'Outline variation','网格与材料':'Grid & material',
'厚度网格数':'Cells through thickness','8–32；先用 12':'8–32; start with 12','模拟时长':'Duration','导热系数':'Conductivity','密度':'Density','比热容':'Heat capacity',
'三维浸没层方法':'3D immersed layers',
'闭合曲面网格描述牛排，独立的三维笛卡尔网格计算内部瞬态导热。形状是可调的圆润近似，并非实物扫描。':'A closed surface mesh represents the steak. A separate 3D Cartesian grid solves transient conduction. The adjustable rounded shape is an approximation, not a scan.',
'锅面、对流与翻面':'Pan contact, convection & flips','表面向内热流：':'Inward surface heat flux:',
'q = w h锅 (T锅 − T表面)':'q = w h_pan (T_pan − T_surface)',
'+ (1 − w) h空气 (T空气 − T表面)':'+ (1 − w) h_air (T_air − T_surface)',
'接触比例 w 由底部接触带深度和朝下法向平滑确定。接触带设为 0 可关闭锅面换热。每次翻面绕长度方向旋转 180°，接触面切换，牛排内部温度场连续。锅温和气温保持设定值。':'The contact fraction w smoothly depends on depth in the bottom contact band and the downward normal. Set the band to 0 to disable pan contact. Each flip rotates the steak 180° about its length, changing the contacting face while keeping the interior temperature field continuous. Pan and air temperatures remain constant.',
'计算实现':'Numerical implementation','T = T初始 + u/H':'T = T_initial + u/H',
'使用四点正则化 delta 函数、交错网格双层算子和校准的表面温度重构。与 Android 版采用同一离散算法，使用双精度数值。它是独立 immersed-layer 变体，不是 JuliaIBPM 原始 Neumann 约束算法。':'Uses a four-point regularized delta, a staggered-grid double-layer operator and calibrated surface-temperature reconstruction. The double-precision discretization matches the Android version. This is an independent immersed-layer variant, not JuliaIBPM’s reference Neumann-constraint algorithm.',
'如何解读':'Interpreting the results',
'显式时间步在翻面时刻精确截断。球体对流解析解测试和 Android 跨版本比较用于核验实现；粗网格的加密误差不一定单调。辅助域交换的数值热量会单独报告，不能将很小的能量记账残差理解为准确的物理预测。':'Explicit time steps end exactly at flip events. Analytical Robin-sphere tests and Android comparisons verify the implementation; coarse-grid refinement errors need not be monotonic. Numerical auxiliary-box heat exchange is reported separately. A small energy-accounting residual does not imply an accurate physical prediction.',
'不模拟蒸发、辐射、结壳、流体运动或接触形变，也不作食品安全判断。材料与换热默认值仅供演示。':'Evaporation, radiation, crust, fluid motion and contact deformation are not modeled. This is not a food-safety prediction. Default material and heat-transfer values are illustrative.',
'保留与导出':'Saving & exporting',
'设置保存在当前设备。切到后台会暂停；刷新或关闭后不恢复计算结果。温度和坐标导出为 CSV，网格为毫米单位的 STL。':'Settings are saved on this device. The run pauses in the background; results are not restored after refreshing or closing. Export temperatures and coordinates as CSV, or the mesh as STL in millimetres.',
'添加到 iPhone 主屏幕':'Add to iPhone Home Screen','本地计算 · 研究模型':'Local computation · research model','安装到 iPhone':'Install on iPhone',
'在 Safari 中打开本页面。':'Open this page in Safari.','点“分享”，选择“添加到主屏幕”。':'Tap Share, then Add to Home Screen.',
'如有“作为网页 App 打开”，保持开启，然后点“添加”。':'If Open as Web App is shown, leave it enabled, then tap Add.',
'这是主屏幕网页 App，无需安装 IPA。首次打开需联网；离线可用性取决于页面缓存及 iOS 存储策略。':'This is a Home Screen web app; no IPA installation is needed. The first visit requires internet access. Offline availability depends on caching and iOS storage policies.',
'这是主屏幕网页 App，无需安装 IPA。首次打开需联网和账户访问；离线可用性取决于页面缓存及 iOS 存储策略。':'This is a Home Screen web app; no IPA installation is needed. The first visit requires internet and account access. Offline availability depends on caching and iOS storage policies.',
'知道了':'Got it','已取消':'Canceled','正在创建三维网格…':'Building the 3D grid…','正在初始化':'Initializing',
'无法启动计算。':'Unable to start the simulation.','计算未完成':'Simulation interrupted','暂停':'Pause','继续':'Resume','模拟完成':'Complete','正在计算':'Running','已暂停':'Paused','重新计算':'New run','无':'None',
'尺寸、材料参数和时间必须为正数。':'Dimensions, material properties and duration must be positive and finite.',
'尺寸范围 5–500 mm；厚度不能超过长或宽。':'Use dimensions from 5–500 mm; thickness cannot exceed length or width.',
'厚度方向网格数须为 8–32 的整数。':'Use an integer from 8–32 for cells through thickness.',
'温度须在 −1000 至 1000 °C。':'Temperatures must be between −1000 and 1000 °C.',
'换热系数须为 0–20000 W/(m² K)。':'Transfer coefficients must be 0–20000 W/(m² K).',
'接触带须为 0 至厚度的一半；0 表示关闭锅面接触。':'The contact band must be between 0 and half the thickness; 0 disables pan contact.',
'圆润指数范围 2–4，轮廓变化范围 0–0.15。':'Use a roundness exponent of 2–4 and outline variation of 0–0.15.',
'时长须为 1e−6 至 86400 秒。':'Duration must be between 1e−6 and 86400 seconds.','最多设置 100 次翻面。':'Use at most 100 flip times.',
'翻面时间须为正数、严格递增，且不超过 86400 秒。':'Flip times must be positive, strictly increasing and at most 86400 seconds.',
'时间步数过多，请降低时长、网格分辨率或换热系数。':'Too many time steps. Reduce duration, resolution or transfer coefficients.',
'网格超过 250,000 个单元，请降低分辨率或长宽比。':'The grid exceeds 250,000 cells. Reduce resolution or aspect ratio.',
'表面超过 4,000 个标记，请降低分辨率。':'The surface exceeds 4,000 markers. Reduce resolution.',
'无可解析的内部区域，请提高厚度网格数。':'No resolved core. Increase cells through thickness.',
'未解析出锅面接触点，请增加接触带或分辨率。':'No resolved contact markers. Increase the contact band or resolution.',
'表面掩膜无法解析。':'Invalid surface mask trace.','掩膜求解未收敛。':'The mask solve did not converge.','形状分辨率不足。':'Unresolved geometry mask.',
'解出现非有限值，请降低分辨率或检查参数。':'The solution contains non-finite values. Reduce resolution or check parameters.',
'请先完成网格初始化。':'Wait for the grid to finish initializing.','未知导出类型':'Unknown export format'
};
const patterns=[
 [/^([AB]) 面朝下$/,(_,s)=>`Side ${s} down`],
 [/^已翻面 (\d+) 次$/,(_,n)=>`${n} ${n==='1'?'flip':'flips'}`],
 [/^冷点 x (.+)$/,(_,s)=>`Cold point x ${s}`],
 [/^(.*) mm · 牛排坐标$/,(_,s)=>`${s} mm · body coordinates`],
 [/^(.*) 网格 · (\d+) 表面标记$/,(_,g,n)=>`${g} grid · ${n} surface markers`],
 [/^网格间距 (.*) mm · 步长 ≤ (.*) s$/,(_,h,dt)=>`Grid spacing ${h} mm · time step ≤ ${dt} s`],
 [/^已计算 (\d+) 步$/,(_,n)=>`${n} steps`],
 [/^内部平均 (.*) °C · 最高 (.*) °C$/,(_,mean,max)=>`Core mean ${mean} °C · max ${max} °C`],
 [/^掩膜能量 (.*) J$/,(_,v)=>`Masked energy ${v} J`],
 [/^边界累计输入 (.*) J$/,(_,v)=>`Boundary input ${v} J`],
 [/^数值计算域交换 (.*) J$/,(_,v)=>`Auxiliary-domain exchange ${v} J`],
 [/^能量记账残差 (.*) J$/,(_,v)=>`Energy accounting residual ${v} J`],
 [/^导出失败：(.*)$/,(_,v)=>`Export failed: ${translate(v,'en')}`]
];
function translate(value,language){if(language!=='en')return value;return value.split('\n').map(line=>{const source=line.trim();let result=en[source];if(result===undefined){result=source;for(const [pattern,replace] of patterns)if(pattern.test(source)){result=source.replace(pattern,replace);break;}}return line.replace(source,()=>result);}).join('\n');}
function chooseLanguage(saved,languages){return saved==='en'||saved==='zh'?saved:(languages?.[0]||'en').toLowerCase().startsWith('zh')?'zh':'en';}
const api={translate,chooseLanguage,en};if(typeof module!=='undefined'&&module.exports){module.exports=api;return;}
root.HeatLabI18n=api;
let saved;try{saved=localStorage.getItem('heat-lab-language');}catch{}
let language=chooseLanguage(saved,navigator.languages||[navigator.language]);
const texts=new WeakMap(),attributes=new WeakMap();
function updateText(node){if(node.parentElement?.closest('script,style,textarea,[data-no-translate]'))return;const prior=texts.get(node),source=prior&&node.nodeValue===prior.output?prior.source:node.nodeValue;const output=translate(source,language);texts.set(node,{source,output});if(node.nodeValue!==output)node.nodeValue=output;}
function updateAttributes(element){let record=attributes.get(element);if(!record){record={};attributes.set(element,record);}for(const name of ['aria-label','placeholder','content']){if(!element.hasAttribute(name))continue;const value=element.getAttribute(name),prior=record[name],source=prior&&value===prior.output?prior.source:value,output=translate(source,language);record[name]={source,output};if(value!==output)element.setAttribute(name,output);}}
function translateTree(node){if(node.nodeType===3){updateText(node);return;}if(node.nodeType!==1&&node.nodeType!==9)return;if(node.nodeType===1)updateAttributes(node);const walker=document.createTreeWalker(node,NodeFilter.SHOW_ELEMENT|NodeFilter.SHOW_TEXT);while(walker.nextNode()){const n=walker.currentNode;if(n.nodeType===3)updateText(n);else updateAttributes(n);}}
function applyLanguage(){document.documentElement.lang=language==='zh'?'zh-CN':'en';document.getElementById('language').value=language;document.querySelector('link[rel=manifest]').setAttribute('href',language==='en'?'./manifest.en.webmanifest':'./manifest.webmanifest');translateTree(document.documentElement);}
const observer=new MutationObserver(records=>{for(const record of records){if(record.type==='characterData')updateText(record.target);else if(record.type==='attributes')updateAttributes(record.target);else for(const node of record.addedNodes)translateTree(node);}});
observer.observe(document.documentElement,{subtree:true,childList:true,characterData:true,attributes:true,attributeFilter:['aria-label','placeholder','content']});
document.getElementById('language').addEventListener('change',event=>{language=event.target.value==='zh'?'zh':'en';try{localStorage.setItem('heat-lab-language',language);}catch{}applyLanguage();});
applyLanguage();
})(globalThis);

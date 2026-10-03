const assert=require('node:assert/strict'),fs=require('node:fs');
const {translate,chooseLanguage}=require('../../ios-web/dist/i18n.js');
const chinese=/[\u3400-\u9fff]/;
assert.equal(chooseLanguage(null,['en-US']),'en');assert.equal(chooseLanguage(null,['zh-TW']),'zh');assert.equal(chooseLanguage('en',['zh-CN']),'en');assert.equal(chooseLanguage('zh',['en']),'zh');assert.equal(chooseLanguage('bad',[]),'en');
const html=fs.readFileSync('ios-web/dist/index.html','utf8');
for(const match of html.matchAll(/>([^<>]+)</g)){const source=match[1];if(chinese.test(source)&&source.trim()!=='中文')assert(!chinese.test(translate(source,'en')),`Untranslated HTML: ${source}`);assert.equal(translate(source,'zh'),source);}
for(const file of ['solver.js','export.js']){const code=fs.readFileSync('ios-web/dist/'+file,'utf8');for(const match of code.matchAll(/Error\('([^']+)'\)/g))assert(!chinese.test(translate(match[1],'en')),`Untranslated error: ${match[1]}`);}
assert.equal(translate('已翻面 1 次','en'),'1 flip');assert.equal(translate('已翻面 2 次','en'),'2 flips');assert.equal(translate('B 面朝下','en'),'Side B down');
const diagnostics='74 × 54 × 22 网格 · 1748 表面标记\n网格间距 2.083 mm · 步长 ≤ 1.33 s\n已计算 500 步\n内部平均 80.83 °C · 最高 120.13 °C\n掩膜能量 51175.4 J\n边界累计输入 53633.8 J\n数值计算域交换 -2458.3 J\n能量记账残差 5.6e-10 J';
assert(!chinese.test(translate(diagnostics,'en')));assert.equal(translate(diagnostics,'en').match(/-?\d+(?:\.\d+)?(?:e-\d+)?/g).join(),diagnostics.match(/-?\d+(?:\.\d+)?(?:e-\d+)?/g).join());
assert.equal(translate('导出失败：请先完成网格初始化。','en'),'Export failed: Wait for the grid to finish initializing.');
console.log('PASS English HTML/error coverage, language selection and dynamic diagnostic values');

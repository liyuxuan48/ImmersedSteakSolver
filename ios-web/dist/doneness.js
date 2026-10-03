/* Rounded Celsius targets: https://www.certifiedangusbeef.com/en/degree-of-doneness
 * Targets are used as lower cutoffs for this visualization, not validated kinetics. */
(function(root){'use strict';
const thresholds=[52,57,63,66,71];
const colors=['#526c91','#ad2446','#df5870','#f29482','#cda27e','#866349'];
function category(peak){if(!Number.isFinite(peak))return -1;let i=0;while(i<thresholds.length&&peak>=thresholds[i])i++;return i;}
function color(peak){const i=category(peak);return i<0?'#666666':colors[i];}
const api={thresholds,colors,category,color};if(typeof module!=='undefined'&&module.exports)module.exports=api;else root.HeatLabDoneness=api;
})(globalThis);

/* Independent JavaScript port of Heat Lab's Java immersed-layer solver.
 * Units: metres, seconds, degrees C. Float64 state; planar steak faces and exact flip events.
 * Calibrated trace closure, not the reference Julia Neumann-constraint algorithm.
 */
(function(root){
'use strict';
const defaults=()=>({length:.12,width:.08,thickness:.025,k:.45,rho:1050,cp:3500,initial:5,panTemperature:180,airTemperature:25,contactH:500,airH:15,flatFaces:true,end:600,exponent:4,asymmetry:.1,flipTimes:[300],across:12});
const arr=n=>new Float64Array(n),dot=(a,b)=>{let s=0;for(let i=0;i<a.length;i++)s+=a[i]*b[i];return s;};
const sub=(a,b)=>a.map((v,i)=>v-b[i]),cross=(a,b)=>[a[1]*b[2]-a[2]*b[1],a[2]*b[0]-a[0]*b[2],a[0]*b[1]-a[1]*b[0]],norm=a=>Math.sqrt(dot(a,a));
// Compare the normalized outward facet normal with world down, inclusive at 15°.
function panFacing(normal,parity=0){const magnitude=Math.hypot(...normal);return magnitude>0&&-(parity%2?-1:1)*normal[2]/magnitude>=Math.cos(Math.PI/12)-1e-12;}
function validate(c){
 for(const k of ['length','width','thickness','k','rho','cp','end'])if(!Number.isFinite(c[k])||c[k]<=0)throw Error('尺寸、材料参数和时间必须为正数。');
 if(c.length<.005||c.length>.5||c.width<.005||c.width>.5||c.thickness<.005||c.thickness>Math.min(c.length,c.width))throw Error('尺寸范围 5–500 mm；厚度不能超过长或宽。');
 if(!Number.isInteger(c.across)||c.across<8||c.across>32)throw Error('厚度方向网格数须为 8–32 的整数。');
 for(const k of ['initial','panTemperature','airTemperature'])if(!Number.isFinite(c[k])||Math.abs(c[k])>1000)throw Error('温度须在 −1000 至 1000 °C。');
 for(const k of ['contactH','airH'])if(!Number.isFinite(c[k])||c[k]<0||c[k]>20000)throw Error('换热系数须为 0–20000 W/(m² K)。');
 if(!Number.isFinite(c.exponent)||c.exponent<2||c.exponent>4||!Number.isFinite(c.asymmetry)||c.asymmetry<0||c.asymmetry>.15)throw Error('圆润指数范围 2–4，轮廓变化范围 0–0.15。');
 if(c.end<1e-6||c.end>86400)throw Error('时长须为 1e−6 至 86400 秒。');
 if(!Array.isArray(c.flipTimes)||c.flipTimes.length>100)throw Error('最多设置 100 次翻面。');
 let prev=0;for(const t of c.flipTimes){if(!Number.isFinite(t)||t<=prev||t>86400)throw Error('翻面时间须为正数、严格递增，且不超过 86400 秒。');prev=t;}
}
class Geometry{
 constructor(c,spacing){
  this.a=c.length/2;this.b=c.width/2;this.c=c.thickness/2;this.p=c.exponent;this.flatFaces=c.flatFaces!==false;this.asymmetry=c.asymmetry;this.vertices=[];this.quads=[];this.markers=[];
  const radii=[this.a,this.b,this.c];
  for(let axis=0;axis<3;axis++)for(const sign of [-1,1]){
   const d1=(axis+1)%3,d2=(axis+2)%3,n1=Math.max(2,Math.ceil(2*radii[d1]/spacing)),n2=Math.max(2,Math.ceil(2*radii[d2]/spacing)),base=this.vertices.length;
   for(let j=0;j<=n2;j++)for(let i=0;i<=n1;i++){const v=[0,0,0];v[axis]=sign*radii[axis];v[d1]=radii[d1]*(2*i/n1-1);v[d2]=radii[d2]*(2*j/n2-1);this.vertices.push(this.project(v));}
   for(let j=0;j<n2;j++)for(let i=0;i<n1;i++){
    const v0=base+j*(n1+1)+i,v1=v0+1,v3=v0+n1+1,v2=v3+1,q=sign>0?[v0,v1,v2,v3]:[v0,v3,v2,v1];this.quads.push(q);
    const normal=[0,0,0],centre=[0,0,0];let area=0;
    for(let k=0;k<4;k++)for(let d=0;d<3;d++)centre[d]+=this.vertices[q[k]][d]/4;
    for(let k=1;k<=2;k++){const cr=cross(sub(this.vertices[q[k]],this.vertices[q[0]]),sub(this.vertices[q[k+1]],this.vertices[q[0]]));area+=norm(cr)/2;for(let d=0;d<3;d++)normal[d]+=cr[d]/2;}
    this.markers.push([...this.project(centre),area,...normal.map(v=>v/area)]);
   }
  }
 }
 level(x,y,z){const angle=Math.atan2(y/this.b,x/this.a),outline=1+this.asymmetry*(.65*Math.cos(3*angle)+.35*Math.sin(angle));const xy=Math.hypot(x/this.a,y/this.b)/outline;const curved=(xy**this.p+Math.abs((this.flatFaces ? 0.8 : 1)*z/this.c)**this.p)**(1/this.p);return this.flatFaces?Math.max(curved,Math.abs(z/this.c)):curved;}
 project(v){const s=this.level(...v);return v.map(x=>x/s);}
 interiorDistance(x,y,z){const f=this.level(x,y,z),e=Math.min(this.a,this.b,this.c)*1e-4,gx=(this.level(x+e,y,z)-this.level(x-e,y,z))/(2*e),gy=(this.level(x,y+e,z)-this.level(x,y-e,z))/(2*e),gz=(this.level(x,y,z+e)-this.level(x,y,z-e))/(2*e),g=Math.sqrt(gx*gx+gy*gy+gz*gz);return g<1e-12?Math.min(this.a,this.b,this.c):(1-f)/g;}
}
class Operator{
 constructor(m,n){this.ids=Array.from({length:m},()=>new Int32Array(n));this.weights=Array.from({length:m},()=>arr(n));}
 spread(surface,grid){grid.fill(0);for(let s=0;s<this.ids.length;s++)for(let j=0;j<this.ids[s].length;j++)grid[this.ids[s][j]]+=this.weights[s][j]*surface[s];}
 sample(grid,surface){for(let s=0;s<this.ids.length;s++){let v=0;for(let j=0;j<this.ids[s].length;j++)v+=this.weights[s][j]*grid[this.ids[s][j]];surface[s]=v;}}
}
class Solver{
 constructor(config){
  validate(config);const c=this.c=JSON.parse(JSON.stringify(config));this.capacity=c.rho*c.cp;const h=this.h=c.thickness/c.across;this.alpha=c.k/this.capacity;
  this.stableDt=.4/(6*this.alpha/(h*h)+2*Math.max(c.contactH,c.airH)/this.capacity/h);
  if(!Number.isFinite(this.stableDt)||this.stableDt<=0||c.end/this.stableDt>20000)throw Error('时间步数过多，请降低时长、网格分辨率或换热系数。');
  const even=n=>Math.floor((n+1)/2)*2;this.nx=even(Math.ceil(c.length*(1+c.asymmetry)/h)+10);this.ny=even(Math.ceil(c.width*(1+c.asymmetry)/h)+10);this.nz=c.across+10;
  const {nx,ny,nz}=this;this.size=nx*ny*nz;if(this.size>250000)throw Error('网格超过 250,000 个单元，请降低分辨率或长宽比。');
  this.fx=(nx+1)*ny*nz;this.fy=nx*(ny+1)*nz;this.faceSize=this.fx+this.fy+nx*ny*(nz+1);this.x0=-nx*h/2;this.y0=-ny*h/2;this.z0=-nz*h/2;
  this.geometry=new Geometry(c,2*h);const m=this.geometry.markers.length;if(m>4000)throw Error('表面超过 4,000 个标记，请降低分辨率。');
  for(const key of ['scaling','unit','traceMask','surface','robin','environment','flux'])this[key]=arr(m);
  for(const key of ['correction','faces','gradient'])this[key]=arr(this.faceSize);
  for(const key of ['mask','u','work','gridTmp'])this[key]=arr(this.size);
  this.inside=new Uint8Array(this.size);this.core=new Uint8Array(this.size);this.coreIndices=[];
  for(let z=0;z<nz;z++)for(let y=0;y<ny;y++)for(let x=0;x<nx;x++){const i=this.index(x,y,z);this.inside[i]=this.geometry.level(this.x(x),this.y(y),this.z(z))<1;this.core[i]=this.inside[i]&&this.geometry.interiorDistance(this.x(x),this.y(y),this.z(z))>=2*h;if(this.core[i])this.coreIndices.push(i);}
  if(!this.coreIndices.length)throw Error('无可解析的内部区域，请提高厚度网格数。');
  this.scalar=new Operator(m,64);this.normal=new Operator(m,192);this.contact=[arr(m),arr(m)];const contacts=[0,0];
  for(let s=0;s<m;s++){
   const q=this.geometry.markers[s];this.scaling[s]=Math.sqrt(q[3]/(h*h*h));
   for(let type=-1;type<3;type++){
    const gx=(q[0]-this.x0)/h-(type===0?0:.5),gy=(q[1]-this.y0)/h-(type===1?0:.5),gz=(q[2]-this.z0)/h-(type===2?0:.5);let at=0;
    for(let k=Math.floor(gz)-1;k<=Math.floor(gz)+2;k++)for(let j=Math.floor(gy)-1;j<=Math.floor(gy)+2;j++)for(let i=Math.floor(gx)-1;i<=Math.floor(gx)+2;i++){
     const w=Solver.phi(i-gx)*Solver.phi(j-gy)*Solver.phi(k-gz)*this.scaling[s];
     if(type<0){this.scalar.ids[s][at]=this.index(i,j,k);this.scalar.weights[s][at]=w;}else{this.normal.ids[s][type*64+at]=this.faceIndex(type,i,j,k);this.normal.weights[s][type*64+at]=w*q[4+type];}at++;
    }
   }
   // Flipping reverses the normal's world z component.
   for(let parity=0;parity<2;parity++){
    this.contact[parity][s]=c.contactH>0&&panFacing(q.slice(4,7),parity)?1:0;
    if(this.contact[parity][s])contacts[parity]++;
   }
  }
  if(c.contactH>0&&contacts.some(n=>n===0))throw Error('未解析出符合角度条件的接触面，请提高分辨率或关闭锅面换热。');
  this.normal.spread(this.scaling,this.faces);this.divergence(this.faces,this.gridTmp);this.solveMask(this.gridTmp);
  this.volume=0;for(const a of this.mask)this.volume+=a*h*h*h;
  this.grad(this.mask,this.gradient);const n=norm(this.scaling);for(let s=0;s<m;s++)this.unit[s]=this.scaling[s]/n;for(let i=0;i<this.faceSize;i++)this.correction[i]=(-this.gradient[i]-this.faces[i])/n;
  this.scalar.sample(this.mask,this.traceMask);for(let s=0;s<m;s++){this.traceMask[s]/=this.scaling[s];if(this.traceMask[s]<.1)throw Error('表面掩膜无法解析。');}
  this.peakTemperature=arr(this.size);this.peakTemperature.fill(c.initial);
  this.time=this.steps=this.panPower=this.airPower=this.boxPower=this.inputEnergy=this.boxEnergy=this.initialEnergy=0;this.updateBoundary();this.solveSurface();
 }
 index(x,y,z){return(z*this.ny+y)*this.nx+x;}
 faceIndex(a,x,y,z){return a===0?(z*this.ny+y)*(this.nx+1)+x:a===1?this.fx+(z*(this.ny+1)+y)*this.nx+x:this.fx+this.fy+(z*this.ny+y)*this.nx+x;}
 x(i){return this.x0+(i+.5)*this.h;}y(i){return this.y0+(i+.5)*this.h;}z(i){return this.z0+(i+.5)*this.h;}
 flipsAt(t){let n=0;for(const f of this.c.flipTimes)if(f<=t)n++;else break;return n;}
 nextFlip(){for(const t of this.c.flipTimes)if(t>this.time&&t<this.c.end)return t;return Infinity;}
 contactWeight(s,t=this.time){return this.contact[this.flipsAt(t)%2][s];}
 temperatures(){return this.u.map((v,i)=>this.c.initial+(this.inside[i]?v/this.mask[i]:0));}
 surfaceTemperatures(){return this.surface.map((v,i)=>this.c.initial+v/this.scaling[i]);}
 inwardFluxes(){return this.surface.map((v,i)=>this.capacity*2*this.robin[i]*(this.environment[i]-v/this.scaling[i]));}
 coreStats(){let min=Infinity,max=-Infinity,sum=0,cold=-1;for(const i of this.coreIndices){const t=this.c.initial+this.u[i]/this.mask[i];if(t<min){min=t;cold=i;}max=Math.max(max,t);sum+=t;}return[min,max,sum/this.coreIndices.length,cold];}
 energy(){let sum=0;for(const v of this.u)sum+=v;return this.capacity*this.h*this.h*this.h*sum;}
 energyBalanceError(){return this.energy()-this.initialEnergy-this.inputEnergy-this.boxEnergy;}
 initializeUniform(t){if(!Number.isFinite(t))throw Error('Nonfinite temperature');for(let i=0;i<this.size;i++)this.u[i]=this.mask[i]*(t-this.c.initial);this.peakTemperature.fill(t);this.time=this.steps=this.inputEnergy=this.boxEnergy=0;this.initialEnergy=this.energy();this.updateBoundary();this.solveSurface();}
 static phi(r){const a=Math.abs(r);if(a>=2)return 0;if(a<=1)return(3-2*a+Math.sqrt(1+4*a-4*a*a))/8;return(5-2*a-Math.sqrt(-7+12*a-4*a*a))/8;}
 grad(v,out){const {nx,ny,nz,h}=this;for(let z=0;z<nz;z++)for(let y=0;y<ny;y++)for(let x=0;x<=nx;x++)out[this.faceIndex(0,x,y,z)]=((x<nx?v[this.index(x,y,z)]:0)-(x>0?v[this.index(x-1,y,z)]:0))/h;
  for(let z=0;z<nz;z++)for(let y=0;y<=ny;y++)for(let x=0;x<nx;x++)out[this.faceIndex(1,x,y,z)]=((y<ny?v[this.index(x,y,z)]:0)-(y>0?v[this.index(x,y-1,z)]:0))/h;
  for(let z=0;z<=nz;z++)for(let y=0;y<ny;y++)for(let x=0;x<nx;x++)out[this.faceIndex(2,x,y,z)]=((z<nz?v[this.index(x,y,z)]:0)-(z>0?v[this.index(x,y,z-1)]:0))/h;}
 divergence(v,out){for(let z=0;z<this.nz;z++)for(let y=0;y<this.ny;y++)for(let x=0;x<this.nx;x++)out[this.index(x,y,z)]=(v[this.faceIndex(0,x+1,y,z)]-v[this.faceIndex(0,x,y,z)]+v[this.faceIndex(1,x,y+1,z)]-v[this.faceIndex(1,x,y,z)]+v[this.faceIndex(2,x,y,z+1)]-v[this.faceIndex(2,x,y,z)])/this.h;}
 negativeLaplacian(v,out){const {nx,ny,nz}=this;for(let z=0;z<nz;z++)for(let y=0;y<ny;y++)for(let x=0;x<nx;x++){const i=this.index(x,y,z);out[i]=6*v[i]-(x>0?v[i-1]:0)-(x+1<nx?v[i+1]:0)-(y>0?v[i-nx]:0)-(y+1<ny?v[i+nx]:0)-(z>0?v[i-nx*ny]:0)-(z+1<nz?v[i+nx*ny]:0);}}
 solveMask(layer){const r=arr(this.size),p=arr(this.size),ap=arr(this.size);for(let i=0;i<this.size;i++)p[i]=r[i]=this.h*this.h*layer[i];let rr=dot(r,r);const initial=rr,tol=rr*1e-20;
  for(let it=0;it<1000&&rr>tol;it++){this.negativeLaplacian(p,ap);const a=rr/dot(p,ap);for(let i=0;i<this.size;i++){this.mask[i]+=a*p[i];r[i]-=a*ap[i];}const next=dot(r,r),b=next/rr;rr=next;for(let i=0;i<this.size;i++)p[i]=r[i]+b*p[i];}
  this.maskResidual=Math.sqrt(rr/initial);if(rr>tol)throw Error('掩膜求解未收敛。');for(const i of this.coreIndices)if(this.mask[i]<.5||this.mask[i]>1.5)throw Error('形状分辨率不足。');}
 spreadNormal(v,out){this.normal.spread(v,out);const sum=dot(this.unit,v);for(let i=0;i<this.faceSize;i++)out[i]+=this.correction[i]*sum;}
 sampleNormal(v,out){this.normal.sample(v,out);const sum=dot(this.correction,v);for(let s=0;s<out.length;s++)out[s]+=this.unit[s]*sum;}
 updateBoundary(){const c=this.c,parity=this.flipsAt(this.time)%2;for(let s=0;s<this.surface.length;s++){const w=this.contact[parity][s],hc=w*c.contactH,ha=(1-w)*c.airH,ht=hc+ha;this.robin[s]=ht/(2*this.capacity);this.environment[s]=ht>0?(hc*(c.panTemperature-c.initial)+ha*(c.airTemperature-c.initial))/ht:0;}}
 solveSurface(){this.grad(this.u,this.gradient);this.scalar.sample(this.u,this.surface);this.panPower=this.airPower=0;for(let s=0;s<this.surface.length;s++){this.surface[s]/=this.traceMask[s];const t=this.c.initial+this.surface[s]/this.scaling[s],w=this.contactWeight(s),area=this.geometry.markers[s][3];this.flux[s]=this.scaling[s]*2*this.robin[s]*(this.environment[s]-this.surface[s]/this.scaling[s]);this.panPower+=w*this.c.contactH*(this.c.panTemperature-t)*area;this.airPower+=(1-w)*this.c.airH*(this.c.airTemperature-t)*area;}}
 advanceTo(target,maxSteps=100){if(!Number.isFinite(target)||target<this.time||target>this.c.end||!Number.isInteger(maxSteps)||maxSteps<1)throw Error('Invalid target');for(let n=0;n<maxSteps&&this.time<target;n++){
  const limit=Math.min(target,this.nextFlip()),remaining=limit-this.time,dt=Math.min(this.stableDt,remaining);if(this.time+dt===this.time)throw Error('Time step below precision');
  this.spreadNormal(this.surface,this.faces);for(let i=0;i<this.faceSize;i++)this.faces[i]+=this.gradient[i];this.divergence(this.faces,this.work);this.scalar.spread(this.flux,this.gridTmp);
  let sum=0;for(let i=0;i<this.size;i++){sum+=this.work[i];this.u[i]+=dt*(this.alpha*this.work[i]+this.gridTmp[i]);if(this.inside[i])this.peakTemperature[i]=Math.max(this.peakTemperature[i],this.c.initial+this.u[i]/this.mask[i]);}
  this.boxPower=this.capacity*this.alpha*this.h*this.h*this.h*sum;this.inputEnergy+=dt*(this.panPower+this.airPower);this.boxEnergy+=dt*this.boxPower;
  for(const i of this.coreIndices)if(!Number.isFinite(this.u[i]))throw Error('解出现非有限值，请降低分辨率或检查参数。');this.time=dt===remaining?limit:this.time+dt;this.steps++;this.updateBoundary();this.solveSurface();
 }return this.time>=target;}
}
const api={panFacing,defaults,validate,Geometry,Solver};if(typeof module!=='undefined'&&module.exports)module.exports=api;else root.HeatLab=api;
})(globalThis);

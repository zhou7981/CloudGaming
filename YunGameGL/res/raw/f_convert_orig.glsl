varying highp vec2 textureCoordinate;
uniform sampler2D yframe;
uniform sampler2D uframe;
uniform sampler2D vframe;
//uniform vec2 

void main(void) {
    highp float nxx, nyy, nx, ny, r=0.0, g=0.0, b=0.0, y=0.0, u=0.0, v=0.0;
    mediump vec3 yuv;
    nxx=textureCoordinate.x;
    nyy=textureCoordinate.y;
 
	if ((nxx <= 0.625) && (nyy <= 0.857)) {// 800/1280; 600/(800-100)
		nx = nxx * 1.25; // 1280/1024
		ny = nyy * 0.683; // 700/1024
		y=texture2D(yframe,vec2(nx,ny)).r;
    	u=texture2D(uframe,vec2(nx,ny)).r;
    	v=texture2D(vframe,vec2(nx,ny)).r;
		
		//y=1.1643*(y-0.0625);
 		u=u-0.5;
 		v=v-0.5;
 		r=y+1.4075*v;
 		g=y-0.3455*u-0.7169*v;
 		b=y+1.779*u;
	}
	
    gl_FragColor=vec4(r, g, b, 1.0);
    //gl_FragColor = vec4(0.0, 0.0, 1.0, 1.0);
};
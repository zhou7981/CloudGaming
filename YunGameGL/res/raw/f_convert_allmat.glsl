varying highp vec2 textureCoordinate;
uniform sampler2D yframe;
uniform sampler2D uframe;
uniform sampler2D vframe;
//uniform vec2 

void main(void) {
    highp float nxx, nyy, nx, ny;
    highp vec4 yuv = vec4(-0.5);
    highp vec4 rgba = vec4(0.0);
    nxx=textureCoordinate.x;
    nyy=textureCoordinate.y;
 
	if ((nxx <= 0.625) && (nyy <= 0.857)) {// 800/1280; 600/(800-100)
		nx = nxx * 1.25; // 1280/1024
		ny = nyy * 0.683; // 700/1024
		yuv.x=texture2D(yframe,vec2(nx,ny)).r;
    	yuv.y=texture2D(uframe,vec2(nx,ny)).r;
    	yuv.z=texture2D(vframe,vec2(nx,ny)).r;
		
		//y=1.1643*(y-0.0625);
 		rgba = mat4(1.0,	         1.0,     1.0,  0.0,
                   0.0,     -0.21482, 2.12798,  0.0,
                   1.28033, -0.38059,     0.0,  0.0,
                   1.28033, -0.59541, 2.12798, -2.0) * yuv;
	}
	
    gl_FragColor=rgba;
    //gl_FragColor = vec4(0.0, 0.0, 1.0, 1.0);
};
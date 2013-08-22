varying highp vec2 textureCoordinate;
uniform sampler2D yframe;
uniform sampler2D uframe;
uniform sampler2D vframe;
//uniform vec2 

void main(void) {
    highp float nxx, nyy, nx, ny;
    highp vec3 yuv = vec4(-0.5);
    highp vec3 rgb = vec3(0.0);
    nxx=textureCoordinate.x;
    nyy=textureCoordinate.y;
 
	if ((nxx <= 0.625) && (nyy <= 0.857)) {// 800/1280; 600/(800-100)
		nx = nxx * 1.25; // 1280/1024
		ny = nyy * 0.683; // 700/1024
		yuv.x=texture2D(yframe,vec2(nx,ny)).r;
    	yuv.y=texture2D(uframe,vec2(nx,ny)).r - 0.5;
    	yuv.z=texture2D(vframe,vec2(nx,ny)).r - 0.5;
		
		//y=1.1643*(y-0.0625);
 		rgb = mat3(      1.0,       1.0,       1.0,
                     0.0, -.21482, 2.12798,
               1.28033, -.38059,       0) * yuv;
	}
	
    gl_FragColor=vec4(rgb, 1.0);
    //gl_FragColor = vec4(0.0, 0.0, 1.0, 1.0);
};
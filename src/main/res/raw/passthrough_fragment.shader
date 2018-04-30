precision mediump float;
uniform sampler2D u_TexScreen;
uniform sampler2D u_TexPointer;
varying vec2 v_TexCoord;

void main() {
     vec4 scr = texture2D(u_TexScreen, v_TexCoord);
     vec4 point = texture2D(u_TexPointer, v_TexCoord);
     gl_FragColor = point*point.a + scr*(1.0 - point.a);
}

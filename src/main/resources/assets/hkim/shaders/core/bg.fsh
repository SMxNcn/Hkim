#version 330
#extension GL_ARB_separate_shader_objects : require

layout(std140) uniform HkimBackground {
    vec4 Time;        // x = elapsed seconds
    vec4 Mouse;       // xy = mouse position (GUI coords)
    vec4 Resolution;  // xy = screen size (GUI coords)
};

layout(location = 0) in vec4 vertexColor;
layout(location = 0) out vec4 fragColor;

mat2 m(float a) {
    float c = cos(a), s = sin(a);
    return mat2(c, -s, s, c);
}

float map(vec3 p) {
    p.xz *= m(Time.x * 0.4);
    p.xy *= m(Time.x * 0.1);
    vec3 q = p * 2.0 + Time.x;
    return length(p + vec3(sin(Time.x * 0.7))) * log(length(p) + 1.0) + sin(q.x + sin(q.z + sin(q.y))) * 0.5 - 1.0;
}

void main() {
    vec2 a = gl_FragCoord.xy / Resolution.y - vec2(0.9, 0.5);
    vec3 cl = vec3(0.0);
    float d = 2.5;

    for (int i = 0; i <= 5; i++) {
        vec3 p = vec3(0.0, 0.0, 4.0) + normalize(vec3(a, -1.0)) * d;
        float rz = map(p);
        float f = clamp((rz - map(p + 0.1)) * 0.5, -0.1, 1.0);
        vec3 l = vec3(0.1, 0.3, 0.4) + vec3(5.0, 2.5, 3.0) * f;
        cl = cl * l + smoothstep(2.5, 0.0, rz) * 0.6 * l;
        d += min(rz, 1.0);
    }

    fragColor = vec4(cl, vertexColor.a);
}

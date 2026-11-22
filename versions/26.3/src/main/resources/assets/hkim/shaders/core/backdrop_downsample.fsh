#version 330
#extension GL_ARB_separate_shader_objects : require

uniform sampler2D Sampler0;

layout(std140) uniform BlurConfig {
    vec4 Info;
    vec4 Rect;
    vec4 Params;
    vec4 Tint;
};

layout(location = 0) in vec2 texCoord;
layout(location = 0) out vec4 fragColor;

void main() {
    vec2 uv = mix(Rect.xy, Rect.zw, texCoord);
    vec2 t = Params.xy;
    vec4 c = texture(Sampler0, uv + vec2(-1.0, -1.0) * t);
    c += texture(Sampler0, uv + vec2(1.0, -1.0) * t);
    c += texture(Sampler0, uv + vec2(-1.0, 1.0) * t);
    c += texture(Sampler0, uv + vec2(1.0, 1.0) * t);
    vec4 avg = c * 0.25;

    avg.a = 1.0;
    fragColor = avg;
}

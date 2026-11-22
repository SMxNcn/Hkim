#version 330

uniform sampler2D Sampler0;

layout(std140) uniform BlurConfig {
    vec4 Info;
    vec4 Rect;
    vec4 Params;
    vec4 Tint;
};

in vec2 texCoord;
out vec4 fragColor;

float sdRoundRect(vec2 p, vec2 halfSize, float r) {
    vec2 q = abs(p) - halfSize + r;
    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - r;
}

void main() {
    vec2 p = texCoord * Info.xy;
    vec2 halfSize = (Rect.zw - Rect.xy) * 0.5;
    vec2 center = (Rect.zw + Rect.xy) * 0.5;
    float r = min(Params.x, min(halfSize.x, halfSize.y));
    float d = sdRoundRect(p - center, halfSize, r);
    float aa = max(fwidth(d), 0.5);

    float alpha = 1.0 - smoothstep(-aa, 0.0, d);
    vec4 color = texture(Sampler0, texCoord) * alpha;
    if (Tint.a > 0.0) {
        color = Tint * alpha + color * (1.0 - Tint.a);
    }
    fragColor = color;
}

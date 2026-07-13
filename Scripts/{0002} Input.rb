
inputs = [
  [:DOWN, :LEFT, :RIGHT, :UP],
  [:A, :B, :C, :X, :Y, :Z, :L, :R],
  [:SHIFT, :CTRL, :ALT],
  [:F5, :F6, :F7, :F8, :F9]
]

b = Bitmap.new(Graphics.width, Graphics.height)
b.font.outline = false
s = Sprite.new
s.bitmap = b
loop do
  Graphics.update
  Input.update
  s.bitmap.fill_rect(s.bitmap.rect, Color.new(255, 255, 255))
  r = Rect.new(0, 0, 96, 24)
  inputs.each_with_index do |col, x|
    col.each_with_index do |inp, y|
      r.x = x*r.width
      r.y = y*r.height
      if Input.press?(inp)
        s.bitmap.fill_rect(r, Color.new(128, 128, 128))
        s.bitmap.font.color = Color.new(0, 0, 0)
        s.bitmap.draw_text(r, inp, 1)
      else
        s.bitmap.font.color = Color.new(128, 128, 128)
        s.bitmap.draw_text(r, inp, 1)
      end
    end
  end
  s.bitmap.draw_text(0, b.height - 48, b.width, 24, "Dir4: #{Input.dir4}")
  s.bitmap.draw_text(0, b.height - 24, b.width, 24, "Dir8: #{Input.dir8}")
end

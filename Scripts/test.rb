
img = Bitmap.new('Graphics/Battlers/Rat')
img.save('test/rat.gif')

b = Bitmap.new(544, 416)

b.font.color = Color.new(255, 0, 0)
b.font.out_color = Color.new(0, 0, 255)
b.font.outline = true
b.font.shadow = true
b.font.name = 'DejaVu Sans'
# for i in 6 .. 96
#   b.font.size = i
#   r = b.text_size("Hello!")
#   if r.height != i
#     p([i, r])
#   end
# end
b.font.size = 96

b.set_pixel(0, 0, Color.new(5, 105, 205, 200))
p(b.get_pixel(0, 0))

white = Color.new(255, 255, 255)
black = Color.new(0, 0, 0)
red = Color.new(255, 0, 0)
blue = Color.new(0, 0, 255)
yellow = Color.new(255, 255, 0)

b.fill_rect(b.rect, white)

rect = Rect.new(0, 0, 100, 100)
rect.x = (b.width - rect.width) / 2
rect.y = (b.height - rect.height) / 2

inner_rect = Rect.new(rect.x + 2, rect.y + 2, rect.width - 4, rect.height - 4)
inner_rect2 = Rect.new(rect.x + 10, rect.y + 10, rect.width - 20, rect.height - 20)

b.fill_rect(rect, black)
b.gradient_fill_rect(inner_rect, blue, yellow, true)
b.stretch_blt(inner_rect2, img, img.rect)

b.draw_text(b.rect, "Hello, Rat!", 1)
#b.hue_change(120)

b.save('test/a.png')

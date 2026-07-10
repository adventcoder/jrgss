
len = 0
loop do
    Graphics.update
    Input.update
    print("\r")
    print(' ' * len)
    s = Input.pressed.inspect
    print("\r")
    print(s)
    len = s.size
    if Input.trigger?(:B)
        msgbox("hoy")
    elsif Input.trigger?(:C)
        if Input.press?(:SHIFT)
            raise 'oops!'
        else
            if Graphics.width == 640
                Graphics.resize_screen(544, 416)
            else
                Graphics.resize_screen(640, 480)
            end
        end
    end
end

import React, { useState } from "react";

const Switch = React.forwardRef(({ className, checked, onCheckedChange, disabled, ...props }, ref) => {
  const [isChecked, setIsChecked] = useState(checked || false);
  
  const handleChange = () => {
    if (disabled) return;
    
    const newValue = !isChecked;
    setIsChecked(newValue);
    
    if (onCheckedChange) {
      onCheckedChange(newValue);
    }
  };
  
  // Usar o valor controlado se fornecido
  const currentChecked = checked !== undefined ? checked : isChecked;
  
  return (
    <div 
      className={`relative inline-flex h-5 w-10 cursor-pointer rounded-full transition-colors 
        ${currentChecked ? 'bg-blue-500' : 'bg-gray-300'} 
        ${disabled ? 'opacity-50 cursor-not-allowed' : ''} 
        ${className || ''}`}
      onClick={handleChange}
      ref={ref}
      role="switch"
      aria-checked={currentChecked}
      tabIndex={disabled ? -1 : 0}
      {...props}
    >
      <span 
        className={`block h-4 w-4 translate-y-[2px] rounded-full bg-white shadow transition-transform 
          ${currentChecked ? 'translate-x-5' : 'translate-x-1'}`}
      />
    </div>
  );
});

Switch.displayName = "Switch";

export { Switch };

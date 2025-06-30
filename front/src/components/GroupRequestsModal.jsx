import React from 'react';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter, DialogDescription } from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import { Avatar, AvatarFallback } from '@/components/ui/avatar';
import { Check, X, User } from 'lucide-react';

const GroupRequestsModal = ({ isOpen, onClose, requests = [], onRespond }) => {
  if (!requests || requests.length === 0) {
    return (
      <Dialog open={isOpen} onOpenChange={onClose}>
        <DialogContent className="sm:max-w-md">
          <DialogHeader>
            <DialogTitle>Solicitações de Grupo</DialogTitle>
            <DialogDescription>
              Gerencie as solicitações de entrada em grupos que você administra
            </DialogDescription>
          </DialogHeader>
          <div className="py-6 text-center text-gray-500">
            Não há solicitações pendentes
          </div>
          <DialogFooter>
            <Button onClick={onClose}>Fechar</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    );
  }

  return (
    <Dialog open={isOpen} onOpenChange={onClose}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>Solicitações de Grupo</DialogTitle>
          <DialogDescription>
            Gerencie as solicitações de entrada em grupos que você administra
          </DialogDescription>
        </DialogHeader>
        <div className="py-4">
          <div className="space-y-4 max-h-[400px] overflow-y-auto">
            {requests.map((request) => (
              <div 
                key={`${request.userId}-${request.groupId}`} 
                className="flex items-center justify-between p-3 border rounded-lg"
              >
                <div className="flex items-center space-x-3">
                  <Avatar className="h-10 w-10">
                    <AvatarFallback className="bg-gray-100 text-gray-600">
                      <User size={20} />
                    </AvatarFallback>
                  </Avatar>
                  <div>
                    <h4 className="font-medium">{request.userName}</h4>
                    <p className="text-sm text-gray-500">
                      Quer entrar no grupo
                    </p>
                    <p className="text-xs text-gray-400">
                      {new Date(request.timestamp).toLocaleString()}
                    </p>
                  </div>
                </div>
                <div className="flex space-x-2">
                  <Button 
                    size="sm" 
                    variant="outline" 
                    className="bg-green-50 text-green-600 border-green-200 hover:bg-green-100"
                    onClick={() => onRespond(request.userId, request.groupId, true)}
                  >
                    <Check size={16} className="mr-1" />
                    Aceitar
                  </Button>
                  <Button 
                    size="sm" 
                    variant="outline" 
                    className="bg-red-50 text-red-600 border-red-200 hover:bg-red-100"
                    onClick={() => onRespond(request.userId, request.groupId, false)}
                  >
                    <X size={16} className="mr-1" />
                    Rejeitar
                  </Button>
                </div>
              </div>
            ))}
          </div>
        </div>
        <DialogFooter>
          <Button onClick={onClose}>Fechar</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
};

export default GroupRequestsModal;
